package com.example.demo.security;
import java.util.Collection;
import java.util.Collections; // For simple roles

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.model.User;
// import java.util.stream.Collectors; // If mapping multiple roles

public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    // Helper method to easily get the database ID
    public String getId() {
        return user.getUserId(); // Assuming your User entity has getId() returning Long
    }

    public String getEmail() {
        return user.getEmail(); // Assuming User entity has getEmail()
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // --- Provide user roles/authorities here ---
        // Example: Simple default role
         return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        // If you have roles stored in your User entity (e.g., Set<Role> roles):
        // return user.getRoles().stream()
        //          .map(role -> new SimpleGrantedAuthority(role.getName()))
        //          .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        // Return null or empty string as password is not used directly for JWT/Google auth
        return null;
    }

    @Override
    public String getUsername() {
        // Typically return the email or a unique username field
        return user.getEmail();
    }

    // --- Account status methods ---
    // Implement specific logic if needed (e.g., check 'enabled' field on User)
    @Override
    public boolean isAccountNonExpired() {
        return true; // Default to true
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Default to true
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Default to true
    }

    @Override
    public boolean isEnabled() {
        return true; // Default to true (or check user.isEnabled() field)
    }

	public User getUser() {
		// TODO Auto-generated method stub
		return user;
	}

    // Optional: equals and hashCode based on user ID or email
}