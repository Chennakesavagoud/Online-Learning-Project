package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entities.Songs;
import com.example.demo.entities.Users;
import com.example.demo.services.SongsService;
import com.example.demo.services.UsersService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsersController 
{
	@Autowired
	UsersService userv;
	
	@Autowired
	SongsService songserv;

	@PostMapping("/register")
	public String addUser(@ModelAttribute Users user) {
		boolean userstatus = userv.emailExists
				(user.getEmail());
		if(userstatus == false) {
			userv.addUser(user);
			return "login";
		}
		else
		{
			return "login";
		}
	}

	@PostMapping("/login")
	public String validateUser(@RequestParam String email,
			@RequestParam String password, HttpSession session)
	{
		//invoking validateUser() in service
		if(userv.validateUser(email, password) == true)
		{
			
			session.setAttribute("email", email);
			//checking whether the user is admin or customer
			if(userv.getRole(email).equals("admin"))
			{
				return "adminhome";
			}
			else
			{
				return "customerhome";
			}
		}
		else
		{
			return "login";
		}
	}
	
	
	@GetMapping("/exploreSongs")
	public String exploreSongs(HttpSession session, Model model) {

	    // Retrieve email from session
	    String email = (String) session.getAttribute("email");

	    // Check if email is null (user is not logged in)
	    if (email == null) {
	        return "redirect:/login";  // Redirect to login if email is not found in session
	    }

	    // Retrieve user based on email
	    Users user = userv.getUser(email);

	    // Check if user is null (user not found in database)
	    if (user == null) {
	        System.out.println("User not found for email: " + email);
	        return "redirect:/login";  // Redirect to login if user is not found
	    }

	    // Check user status to determine which page to display
	    boolean userStatus = user.isPremium();
	    if (userStatus) {
	        List<Songs> songsList = songserv.fetchAllSongs();
	        model.addAttribute("songslist", songsList);
	        return "displaysongs";  // Show songs if user is premium
	    } else {
	        return "payment";  // Redirect to payment page if user is not premium
	    }
	}


}
















