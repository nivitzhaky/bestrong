package utils;


import java.util.ArrayList;
import java.util.List;


public class User {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private Long id;
//
//    @Column(name = "username")
//    private String username;
//
//    @Column(name = "password")
////    @JsonIgnore
//    private String password;
//
//    @Column(name = "first_name")
//    private String firstName;
//
//    @Column(name = "last_name")
//    private String lastName;
//
//    /**
//     * Roles are being eagerly loaded here because
//     * they are a fairly small collection of items for this example.
//     */
//    @ManyToMany(fetch = FetchType.EAGER)
//    @JoinTable(name = "user_role", joinColumns
//            = @JoinColumn(name = "user_id",
//            referencedColumnName = "id"),
//            inverseJoinColumns = @JoinColumn(name = "role_id",
//                    referencedColumnName = "id"))
//    private List<Role> roles;
//

    private Long id;

    private String username;

    private String password;

    private String firstName;

    private String lastName;

	private String email;

    private List<Role> roles;

	private boolean accountNonExpired;
	private boolean accountNonLocked;
	private boolean credentialsNonExpired;
	private boolean enabled;

	private SocialMediaService signInProvider;


    public User() {
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void grantAuthority(Role authority) {
        if ( roles == null ) roles = new ArrayList<>();
        roles.add(authority);
    }
    public List<Role> getRoles() {
        return roles;
    }
    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }
    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }
    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

	public SocialMediaService getSignInProvider() {
		return signInProvider;
	}

	public void setSignInProvider(SocialMediaService signInProvider) {
		this.signInProvider = signInProvider;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", password=" + password + ", firstName=" + firstName
				+ ", lastName=" + lastName + ", email=" + email + ", roles=" + roles + ", accountNonExpired="
				+ accountNonExpired + ", accountNonLocked=" + accountNonLocked + ", credentialsNonExpired="
				+ credentialsNonExpired + ", enabled=" + enabled + "]";
	}

}