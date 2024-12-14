package com.example.demo.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.entities.Users;
import com.example.demo.services.UsersService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.servlet.http.HttpSession;
import java.util.logging.Logger;

@Controller
public class PaymentController {

    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.secret.key}")
    private String razorpaySecret;

    @Autowired		
    private UsersService service;

    // Create order for Razorpay
    @PostMapping("/createOrder")
    @ResponseBody
    public String createOrder() {
        try {
            // Initialize Razorpay client with credentials
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpaySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", 50000);  // Amount in paise (500.00 INR)
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt#1");

            // Create the order
            Order order = razorpay.orders.create(orderRequest);
            logger.info("Order created successfully: " + order);
            return order.toString();

        } catch (RazorpayException e) {
            logger.severe("Exception while creating order: " + e.getMessage());
            e.printStackTrace(); // For debugging, remove in production
            return "{\"error\":\"Failed to create order\"}";
        }
    }

    // Verify the payment after completion
    @PostMapping("/verify")
    @ResponseBody
    public boolean verifyPayment(@RequestParam String orderId, 
                                  @RequestParam String paymentId,
                                  @RequestParam String signature,
                                  HttpSession session) {

        // Log the received parameters for debugging
        logger.info("Received orderId: " + orderId);
        logger.info("Received paymentId: " + paymentId);
        logger.info("Received signature: " + signature);

        if (orderId == null || paymentId == null || signature == null) {
            logger.warning("Missing required parameters: orderId, paymentId, or signature.");
            return false;
        }

        try {
            // Construct the verification data string
            String verificationData = orderId + "|" + paymentId;

            // Verify the signature
            boolean isValidSignature = Utils.verifySignature(verificationData, signature, razorpaySecret);
            if (isValidSignature) {
                logger.info("Payment verified successfully for order ID: " + orderId);

                // Set the user as premium in the session
                session.setAttribute("paymentVerified", true);

                return true;
            } else {
                logger.warning("Payment verification failed for order ID: " + orderId);
                return false;
            }
        } catch (RazorpayException e) {
            logger.severe("Exception during payment verification: " + e.getMessage());
            return false;
        }
    }

    // Payment Success: Update user as premium and show the songs
    @GetMapping("/payment-success")
    public String paymentSuccess(HttpSession session) {
        Boolean paymentVerified = (Boolean) session.getAttribute("paymentVerified");
        logger.info("Payment verified: " + paymentVerified);

        if (paymentVerified != null && paymentVerified) {
            String email = (String) session.getAttribute("email");
            logger.info("User email from session: " + email);

            if (email == null) {
                logger.warning("Session does not contain an email attribute. Redirecting to login.");
                return "login"; // Ensure this view is correctly mapped
            }

            Users user = service.getUser(email);
            if (user == null) {
                logger.warning("User not found for email: " + email);
                return "login"; // Ensure this view is correctly mapped
            }

            // If the user is already premium, show the songs
            if (user.isPremium()) {
                logger.info("User " + email + " is already a premium member.");
                return "displaysongs";  // Ensure this view is correct
            } else {
                // Set the user as premium
                user.setPremium(true);
                service.updateUser(user);
                logger.info("User " + email + " updated to premium.");

                // Redirect to the songs display page
                return "displaysongs";  // Ensure this view is correct
            }
        }

        logger.warning("Payment verification failed or payment not confirmed.");
        return "login";  // Ensure this view is correctly mapped
    }

    // Payment Failure: Redirect to login page
    @GetMapping("/payment-failure")
    public String paymentFailure() {
        logger.warning("Payment failed. Redirecting to login page.");
        return "login"; // Redirect to login on payment failure
    }
}
