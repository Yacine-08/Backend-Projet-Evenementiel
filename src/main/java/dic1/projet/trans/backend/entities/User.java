package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.Role;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@ToString(exclude = {"favoriteEvents"})
@EqualsAndHashCode(exclude = {"favoriteEvents"})
public class User implements UserDetails {
    @Id
    private String idUser;

    private String firstName;
    private String lastName;
    
    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String username;
    
    @org.springframework.data.mongodb.core.index.Indexed(unique = true, sparse = true)
    private String email;
    
    @org.springframework.data.mongodb.core.index.Indexed(unique = true, sparse = true)
    private String phoneNumber;

    private String password;
    private LocalDateTime inscriptionDate;
    private String profilePhoto;

    private List<Role> roles = new ArrayList<>();

    @DBRef(lazy = true)
    private List<Event> favoriteEvents = new ArrayList<>();


    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    private boolean enabled;
    private boolean accountNonLocked;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Méthodes pour gérer les favoris
    public void addFavorite(Event event) {
        if (favoriteEvents == null) {
            favoriteEvents = new ArrayList<>();
        }
        if (!favoriteEvents.contains(event)) {
            favoriteEvents.add(event);
        }
    }

    public void removeFavorite(Event event) {
        if (favoriteEvents != null) {
            favoriteEvents.remove(event);
        }
    }

    public boolean hasFavorite(Event event) {
        return favoriteEvents != null && favoriteEvents.contains(event);
    }

    public List<Event> getFavoriteEvents() {
        return favoriteEvents != null ? favoriteEvents : new ArrayList<>();
    }
}