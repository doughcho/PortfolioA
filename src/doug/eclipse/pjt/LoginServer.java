package doug.eclipse.pjt;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;				// added for Cookie
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.*;                				// added for JDBC

@WebServlet("/LoginServer")    // or define in the WEB-INF>lib>web.xml
public class LoginServer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	String userName = null;	   // used in the doPost() and authenticationCheck()
       
    public LoginServer() {
        super();
    }
    
    public void init(ServletConfig config) throws ServletException {
    	System.out.println("init() of LoginServer");
    }
    
    public void destroy() {
    	System.out.println("destroy() of LoginServer");
    }

//  Handles MySQL database through JDBC
//  Check if user id and password is valid and active
	protected int authenticationCheck(String userID, String password) throws SQLException {
		String url      = "jdbc:mysql://localhost:3306/userdb";
	    String sysuser  = "root";
	    String syspswd  = "DougsMySQL";
	    Connection conn = null;
	    Statement stmt  = null;
	    ResultSet rs    = null;
	    String sql      = null;
	    
	    int status = 0;    // 0: user does not exist, 1: password not match, 2: inactive user, 3: active user	       
	    try { 
	    	Class.forName("com.mysql.cj.jdbc.Driver");    // load Driver
	    	conn = DriverManager.getConnection(url, sysuser, syspswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_READ_ONLY, 0);      
	        sql = "Select * from users where USER_ID = '"+userID+"';";             	
	        rs = stmt.executeQuery(sql);
	        if (rs.next()) {
	        	status++;    // user exists
	        	if (rs.getString("PASSWORD").equals(password)) {
	        		status++;    // password match
	        		if (rs.getBoolean("ISACTIVE")) status++;    // active user
	        	}
	        	userName = rs.getString("NAME");
	        }
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return status;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		response.getWriter().append("Served at: ").append(request.getContextPath());
		doPost(request, response);		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String msg = "not processed correctly...";

		System.out.println("doPost() of LoginServer");		        
		int status = 0;
		try {
			// Checking user ID and password is valid
			String userID = request.getParameter("userID");
			String passwd = request.getParameter("passwd");
//			System.out.println("User ID is " + userID + ", password is " + passwd);
			userName = "John Doe";
			status = authenticationCheck(userID, passwd);
			switch (status) {
				case 0:
				case 1: {
					msg = "User ID or password not match";
					break;
				}
				case 2: {
					msg = "Inactive user";
					break;
				}
				case 3: {
					msg = "Welcome";
					break;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			System.out.println(msg + ", status = " + status + ", name = " + userName);
			// add cookies (do not work with space in the middle of the cookie value)
			response.setContentType("text/plain;charset=UTF-8");    // ISO-8859-1; or text/http??			
			String path = request.getContextPath();		
			String strStatus = Integer.toString(status);
			Cookie cookie = new Cookie("authStat", strStatus);
			cookie.setPath(path);   // set cookie's usage range
			cookie.setMaxAge(-1);	// do not delete until browser finishes	
			response.addCookie(cookie);
			String name = userName.replace(' ',  '_');    // Cookie value should not include space
			cookie = new Cookie("userName", name);
			cookie.setPath(path);
			cookie.setMaxAge(-1);
			response.addCookie(cookie);
			System.out.println("ending doPost()");
		}
    }
}