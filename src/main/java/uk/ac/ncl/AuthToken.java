package uk.ac.ncl;

public class AuthToken {
    public String uri;
    public String userName;
    public String password;
    public AuthToken(String uri, String u, String p) {
        this.uri = uri;
        userName = u;
        password = p;
    }
}
