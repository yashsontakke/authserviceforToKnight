package com.example.demo.security;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import com.example.demo.model.User;
import com.example.demo.repository.UserDynamoDbRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired // Or use constructor injection
    private UserDynamoDbRepository userDynamoDbRepository;

    // This method is called by Spring Security when it needs user details
    // The 'username' parameter will actually be the User ID extracted from the JWT subject
    @Override

    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException { // ID is now String (UUID)
        // No need to parse to Long anymore
        User user = userDynamoDbRepository.findById(userId) // Use new repo method
            .orElseThrow(() ->
                new UsernameNotFoundException("User not found with id : " + userId)
            );
        return new UserPrincipal(user);
    }
    // You might add another method if needed for loading by email during initial login/lookup
    // but this one is specifically for loading by ID from the JWT subject.
}
