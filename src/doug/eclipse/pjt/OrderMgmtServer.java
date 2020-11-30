package doug.eclipse.pjt;

import java.io.IOException;
import java.io.PrintWriter;    					// added for writing response data

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import javax.servlet.RequestDispatcher;    	// added for forward()
import javax.servlet.http.Cookie;    			// added for Cookie

import com.google.gson.Gson;					// added for JSON (Gson)
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
//import com.google.gson.JsonArray;				// substituted (GsonBuilder)
import com.google.gson.JsonElement;	
import com.google.gson.JsonParser;	
import com.google.gson.JsonParseException;
//import com.mysql.cj.xdevapi.JsonParser;		// added for MySQL <-> JSON interface

import java.sql.*;                				// added for JDBC
import java.text.DateFormat;					// added for Date
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

import java.text.ParseException;
//import java.util.Arrays;						// related to JsonArray

@WebServlet("/OrderMgmtServer")
public class OrderMgmtServer extends HttpServlet {
	private static final long serialVersionUID = 1L;
    static final float TAX_RATE = 0.0785f;
	class Customer {
		int custID;			// primary key
		String name;
		int emplID;
		String addr;
		boolean taxExmt;
		String shipMthd;
		String emplName;	// JOIN (employees)
	}
	class Order {
    	int orderID;		// primary key
    	int custID;
    	int emplID;			// default: from customers
    	String orderDT;
    	String comment;
    	String status;		// order status (set by another processes)
    	String shipMthd;	// default: from customers
       	String shipAddr;	// default: from customers
        String shipPayr;	
    	String trackNum;	// link to shipment tracking system
    	String crName;		
    	String crDT;
    	String updName;
    	String updDT;
    	int     itemCnt;
    	float   subTotal;	
       	float   taxAmt;
        float   shipChrg;
    	float   dcAmt;
    	float   paidAmt;
    	boolean taxExmt;   	// from customers
    	String custName;	// JOIN (customers)
    	String emplName;	// JOIN (employees)
    }
    class OrderItem {		
		int orderID;		// primary key (foreign key to orders)
    	int seq;			// primary key with orderID
		int itemID;
		int qty;
		float uPrice;
		String comment;
		String updDT;
		String itemName;    // JOIN (items)
    }
    class OneOrder {		// Order + OrderItems[itemCnt]
    	Order ord;
    	OrderItem[] ordItem;
    	public OneOrder(int count) {
    		this.ord = new Order();
    		if (count > 0) {
    			this.ordItem = new OrderItem[count];	// reference to objects
    			for (int i = 0; i < count; i++) this.ordItem[i] = new OrderItem();
    		}
    		this.ord.itemCnt = count;
    	}
    }
    class OrderDate {
    	int orderID;
    	String date;
    }
    class CustOrder {		// customer's recent orders
    	int custID;
    	int ordCnt;
    	OrderDate[] cOrds;
    	public CustOrder(int ID, int count) {
    		if (count > 0) {
    			this.cOrds = new OrderDate[count];	// reference to objects
    			for (int i = 0; i < count; i++) this.cOrds[i] = new OrderDate();
    		}
    		this.custID = ID;
    		this.ordCnt = count;
    	}
    }
    class Summary {
    	int    orderID;
    	int    seq;			// for an orderItem added (15)
    	int    itemCnt;
    	float  subTotal;	
    	float  taxAmt;
    	String updName;
    	String updDT;
    	public Summary(int oID, int iseq) {
    		this.orderID  = oID;
    		this.seq      = iseq;
    		this.itemCnt  = 0;
    		this.subTotal = 0;
    		this.taxAmt   = 0;
    		this.updName  = "";
    		this.updDT    = "";
    	}
    }
    Customer  customer 	= null;
    Order	  order    	= null;
    OrderItem orderItem	= null;
    OneOrder  oneOrder  = null;
    CustOrder custOrder = null;
    Summary	  summary   = null;
    String requestData  = null;	// request data (JSON string)
    String responseData = null;	// response data (JSON string)
    String whatEvent    = null;
    String userName     = null;
    static String today;
    // need to be changed to the outside the code
    static String jdbcDriver = "com.mysql.cj.jdbc.Driver";
    static String dbUrl      = "jdbc:mysql://localhost:3306/testdb";
    static String dbUser     = "testUser";
    static String dbPswd     = "4dbAccess@app";
    Connection conn = null;
    Statement  stmt = null;
    ResultSet  rs   = null;
    
    public OrderMgmtServer() {
        super();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = Calendar.getInstance().getTime();
        today = dateFormat.format(date);
    }

	public void init(ServletConfig config) throws ServletException {
    	System.out.println("init() of OrderMgmtServer");
    	try {
			Class.forName(jdbcDriver);	// load JDBC Driver
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void destroy() {
    	System.out.println("destroy() of OrderMgmtServer");
	}

//  Handles MySQL database through JDBC
	protected boolean readOrder(int orderID) throws SQLException {		
		boolean isGood = false;	
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_READ_ONLY, 0);

	    	String sql = "Select * from orders where ORDER_ID="+orderID+";";
	        rs = stmt.executeQuery(sql);
	        if (!rs.next()) return false;
	        int count = rs.getInt("ITEM_CNT");
	        oneOrder = new OneOrder(count);
	        oneOrder.ord.orderID   = rs.getInt("ORDER_ID");
	        oneOrder.ord.custID    = rs.getInt("CUST_ID");
	        oneOrder.ord.emplID    = rs.getInt("EMPL_ID");
	        oneOrder.ord.orderDT   = rs.getString("ODATE");
	        oneOrder.ord.comment   = rs.getString("COMMT");
	        oneOrder.ord.status	   = rs.getString("OSTAT");
	        oneOrder.ord.shipMthd  = rs.getString("SMTHD");
	        oneOrder.ord.shipAddr  = rs.getString("SADDR");
	        oneOrder.ord.shipPayr  = rs.getString("SPAYR");
	        oneOrder.ord.trackNum  = rs.getString("TRACK");
	        oneOrder.ord.crName    = rs.getString("CRNM");
	        oneOrder.ord.crDT      = rs.getString("CRDT");
	        oneOrder.ord.updName   = rs.getString("UPDNM"); 
	        oneOrder.ord.updDT     = rs.getString("UPDDT");
	    	oneOrder.ord.subTotal  = rs.getFloat("STOT");
	    	oneOrder.ord.taxAmt    = rs.getFloat("TAX");
	    	oneOrder.ord.shipChrg  = rs.getFloat("SHCHG");
	    	oneOrder.ord.dcAmt     = rs.getFloat("DCAMT");
	    	oneOrder.ord.paidAmt   = rs.getFloat("PAMT");
	    	oneOrder.ord.taxExmt   = rs.getBoolean("TXEMT");
	    	
	        sql = "Select NAME from customers where CUST_ID="+oneOrder.ord.custID+";";
	        rs = stmt.executeQuery(sql);
	        oneOrder.ord.custName = (rs.next()) ? rs.getString(1) : null;	        
	        sql = "Select NAME from employees where EMPL_ID="+oneOrder.ord.emplID+";";
	        rs = stmt.executeQuery(sql);
	        oneOrder.ord.emplName = (rs.next()) ? rs.getString(1) : null;
	        
	        sql = "Select * from orderitems where ORDER_ID = "+orderID+";";
	        rs = stmt.executeQuery(sql);
	        for (int i = 0; i < count; i++) {
	        	if (!rs.next()) break;
	        	oneOrder.ordItem[i].orderID= orderID;
	        	oneOrder.ordItem[i].seq    = rs.getInt("SEQ");
	        	oneOrder.ordItem[i].itemID = rs.getInt("ITEM_ID");
	        	oneOrder.ordItem[i].qty    = rs.getInt("QTY");
	        	oneOrder.ordItem[i].uPrice = rs.getFloat("PRICE");
	        	oneOrder.ordItem[i].comment= rs.getString("COMMT");
	        }
	        for (int i = 0; i < count; i++) {
	        	sql = "Select NAME from items where ITEM_ID="+oneOrder.ordItem[i].itemID+";";
	        	rs = stmt.executeQuery(sql);
	    	    oneOrder.ordItem[i].itemName = (rs.next()) ? rs.getString(1) : null;
	        }
	        isGood = true;
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}
	
	protected boolean readCustOrders(int custID, int maxCnt) throws SQLException {		
		boolean isGood = false;	
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_READ_ONLY, 0);

	    	String sql = "Select ORDER_ID, ODATE from orders where CUST_ID="+custID+" order by ORDER_ID DESC;";
	        rs = stmt.executeQuery(sql);
	        custOrder = new CustOrder(custID, maxCnt);
	        int cnt = 0;
	        while (rs.next()) {
	        	custOrder.cOrds[cnt].orderID = rs.getInt(1);
	        	custOrder.cOrds[cnt].date    = rs.getString(2);	        	
	        	if (++cnt > maxCnt) break;
	        }
	        custOrder.ordCnt = cnt;
	        isGood = true;
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}

	protected boolean readCust(int custID) throws SQLException {    
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_READ_ONLY, 0);

	    	String sql = "Select * from customers where CUST_ID="+custID+";";
	        rs = stmt.executeQuery(sql);
	        if (!rs.next()) return false;
	        customer = new Customer();
	        customer.custID  = custID;
	        customer.name    = rs.getString("NAME");
	        int emplID 		 = rs.getInt("EMPL_ID");
	        customer.emplID  = emplID;
	        customer.addr	 = rs.getString("ADDR");
	        customer.taxExmt = rs.getBoolean("TXEMT");
	        customer.shipMthd=rs.getString("SMTHD");
	    	
	        sql = "Select NAME from employees where EMPL_ID="+emplID+";";
	        rs = stmt.executeQuery(sql);
	        customer.emplName = (rs.next()) ? rs.getString(1) : null;
	        isGood = true;
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}
	
	protected boolean readEmpl(int emplID) throws SQLException {    
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_READ_ONLY, 0);

	    	String sql = "Select NAME from employees where EMPL_ID="+emplID+";";
	        rs = stmt.executeQuery(sql);
	        if (rs.next()) {
	        	responseData = "{\"emplName\":\""+rs.getString(1)+"\"}";
	        	isGood = true;
	        }
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}
	
	protected boolean readItem(int itemID) throws SQLException {	    
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_READ_ONLY, 0);

	    	String sql = "Select NAME from items where ITEM_ID="+itemID+";";
	        rs = stmt.executeQuery(sql);
	        if (rs.next()) {
	        	responseData = "{\"itemName\":\""+rs.getString(1)+"\"}";
	        	isGood = true;
	        }
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}

	protected boolean insertOrder(Order o) throws SQLException, ParseException {    
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_UPDATABLE, 0);
	    	
	        o.crName = userName;
	        o.crDT   = today;
	    	String sql= "Insert into orders values (NULL,"+o.custID+","+o.emplID+",'";       
	    	sql += o.orderDT+"','"+o.comment+"','1','"+o.shipMthd+"','";
	    	sql += o.shipAddr+"','"+o.shipPayr+"','"+o.trackNum+"','"+o.crName+"','";
	    	sql += o.crDT+"','',NULL,0,0,0,0,0,"+o.taxExmt+",0);";
			System.out.println("sql is " + sql);
	        if (stmt.executeUpdate(sql) != 1) return false;	        
	        rs = stmt.executeQuery("Select last_insert_id();");
	        if (rs.next()) {
	        	o.orderID = rs.getInt(1);         // get generated ORDER_ID
	        	isGood = true;
	        }
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}

	protected boolean updateOrder(Order o) throws SQLException, ParseException {    
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_UPDATABLE, 0);
	    	
	        String sql= "Update orders set ";
	    	switch (whatEvent) {
				case "07" :	// 'Change Order' clicked
		        	o.taxAmt = (o.taxExmt) ? 0 : (float)Math.round(o.subTotal * TAX_RATE * 100) / 100;
					sql += "CUST_ID="+o.custID+",EMPL_ID="+o.emplID+",ODATE='"+o.orderDT+"',";
			    	sql += "COMMT='"+o.comment+"',SMTHD='"+o.shipMthd+"',SADDR='"+o.shipAddr+"',";
			    	sql += "SPAYR='"+o.shipPayr+"',TAX="+o.taxAmt+",TXEMT="+o.taxExmt;
			    	break;
				case "08" :	// 'Cancel Order' clicked
					sql += "OSTAT='9'";
					break;
				case "09" :	// 'Discount' changed
					sql += "DCAMT="+o.dcAmt;
					break;
				case "15" :	// 'Add Item' clicked
				case "16" :	// 'Change Item' clicked
				case "17" :	// 'Delete Item' clicked
			        rs = stmt.executeQuery("Select * from orders where ORDER_ID="+o.orderID+";");
			        if (!rs.next()) return false;
			        summary.itemCnt  += rs.getInt("ITEM_CNT");			
			        summary.subTotal += rs.getFloat("STOT");
		        	summary.taxAmt = (rs.getBoolean("TXEMT")) ? 0 : (float)Math.round(summary.subTotal * TAX_RATE * 100) / 100;
					sql += "STOT="+summary.subTotal+",TAX="+summary.taxAmt+",ITEM_CNT="+summary.itemCnt;
					break;
				default:	// just in case
					return false;
	    	}
    		if (!whatEvent.equals("15")) {
    	    	o.updName = userName;
    			o.updDT   = today;
    			sql += ", UPDNM='"+o.updName+"',UPDDT='"+o.updDT+"'";    	
    		}
	    	sql += " where ORDER_ID="+o.orderID+";";
	    	System.out.println("sql: "+sql);
	        if (stmt.executeUpdate(sql) == 1) isGood = true;
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}

	protected boolean readOrderItem(int orderID, int seq) throws SQLException {		
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_READ_ONLY, 0);
	    	
	        String sql = "Select * from orderitems where ORDER_ID="+orderID+" AND SEQ="+seq+";";
	        rs = stmt.executeQuery(sql);
	        if (!rs.next()) return false;
	        orderItem = new OrderItem();
	        orderItem.orderID = orderID;
	        orderItem.seq     = seq;
	        orderItem.itemID  = rs.getInt("ITEM_ID");
	        orderItem.uPrice  = rs.getFloat("PRICE");
        	orderItem.qty     = rs.getInt("SEQ");
        	orderItem.comment = rs.getString("COMMT");
	        sql = "Select NAME from items where ITEM_ID="+orderItem.itemID+";";
	        rs = stmt.executeQuery(sql);
        	orderItem.itemName = (rs.next()) ? rs.getString(1) : null;
        	isGood = true;
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}

	protected boolean insertOrderItem(OrderItem oi) throws SQLException, ParseException {    
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_UPDATABLE, 0);
	    	
	        String sql = "Select SEQ from orderitems where ORDER_ID = "+oi.orderID+";";
	        rs = stmt.executeQuery(sql);
	        int maxSeq = 0;
	        while (rs.next()) {
	        	int i = rs.getInt(1);
	        	if (i > maxSeq) maxSeq = i;
	        }
	        ++maxSeq;   // for adding orderitems table SEQ 
	    	oi.updDT = today;
	    	sql = "Insert into orderitems values ("+oi.orderID+","+maxSeq+","+oi.itemID+",";    
	    	sql+= oi.uPrice+","+oi.qty+",'"+oi.comment+"','"+oi.updDT+"');";
	        if (stmt.executeUpdate(sql) == 1) {
	        	summary = new Summary(oi.orderID, maxSeq);
	        	summary.itemCnt  = 1;
	        	summary.subTotal = oi.uPrice * oi.qty;
	        	isGood = true;
	        }
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}

	protected boolean updateOrderItem(OrderItem oi) throws SQLException, ParseException {    
		boolean isGood = false;
		try { 
	    	conn = DriverManager.getConnection(dbUrl, dbUser, dbPswd);
	    	stmt = conn.createStatement(ResultSet.CONCUR_UPDATABLE, 0);
	    	
	        String sql = "Select * from orderitems where ORDER_ID="+oi.orderID+" AND SEQ="+oi.seq+";";
	        rs = stmt.executeQuery(sql);
	        if (!rs.next()) return false;
	        summary = new Summary(oi.orderID, oi.seq);
	        summary.subTotal -= rs.getFloat("PRICE") * rs.getInt("QTY");
	        if (whatEvent.equals("16")) {	// 'Change Item' clicked
	        	summary.subTotal += oi.uPrice * oi.qty;
		    	oi.updDT = today;
	        	sql = "Update orderitems set ITEM_ID="+oi.itemID+",PRICE="+oi.uPrice+",";
	        	sql+= "QTY="+oi.qty+",COMMT='"+oi.comment+"',UPDDT='"+oi.updDT+"'";
	        } else {						// 'Delete Item' clicked
	    		summary.itemCnt = -1;
	    		sql = "Delete from orderitems ";
			}
	        sql += " where ORDER_ID="+oi.orderID+" AND SEQ="+oi.seq+";";
	        if (stmt.executeUpdate(sql) == 1) isGood = true;
		} catch (SQLException e) {
	    	System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	    return isGood;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		response.getWriter().append("Served at: ").append(request.getContextPath());
//		System.out.println("doGet() of OrderMgmtServer");		        
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String wellDone = "N";		// for cookie "wellDone"
		int orderID = 0;
		int custID  = 0;

//		System.out.println("doPost() of OrderMgmtServer");		        
		try {
			requestData = request.getParameter("json");
			System.out.println("request data is " + requestData);
			// prepare for the data parsing (common routine)
			@SuppressWarnings("deprecation")
			JsonParser parser = new JsonParser();
			JsonElement element = parser.parse(requestData);		
			JsonObject json = new JsonObject();
			Gson gson = new Gson();	
			// get whatEvent & userName value from cookie
			Cookie[] cookies = request.getCookies();
			for (Cookie cookie:cookies) {
				if (cookie.getName().equals("whatEvent")) whatEvent= cookie.getValue();
				if (cookie.getName().equals("userName")) {
					String uName = cookie.getValue();
					userName = uName.replace('_', ' ');
				}
			}
			switch (whatEvent) {
				case "01":	// 'Order #' inserted
					orderID = element.getAsJsonObject().get("orderID").getAsInt();
					if (readOrder(orderID)) {
						gson = new GsonBuilder().setDateFormat("yyyy-MM-dd' 'HH:mm:ss").create();			
						element = gson.toJsonTree(oneOrder);					
						json = (JsonObject) element;
						responseData = json.toString();
						wellDone = "Y";
					}
					break;
				case "03" :	// 'Customer #' inserted
					custID = element.getAsJsonObject().get("custID").getAsInt();
					if (readCust(custID)) {
						gson = new GsonBuilder().setDateFormat("yyyy-MM-dd' 'HH:mm:ss").create();			
						element = gson.toJsonTree(customer);
						json = (JsonObject) element;		
						responseData = json.toString();
						wellDone = "Y";
					}
					break;
				case "04" :	// 'Recent Orders' clicked
					custID = element.getAsJsonObject().get("custID").getAsInt();
					int maxCnt = element.getAsJsonObject().get("maxCnt").getAsInt();
					if (readCustOrders(custID, maxCnt)) {
						gson = new GsonBuilder().setDateFormat("yyyy-MM-dd' 'HH:mm:ss").create();			
						element = gson.toJsonTree(custOrder);
						json = (JsonObject) element;		
						responseData = json.toString();
						wellDone = "Y";
					}
					break;
				case "05" :	// 'Sales Rep.' inserted
					int emplID = element.getAsJsonObject().get("emplID").getAsInt();
					if (readEmpl(emplID)) wellDone = "Y";
					break;
				case "06" :	// 'Register Order' clicked
					json = (JsonObject) element;
					order = new Order();
					order = gson.fromJson(json, Order.class);
					if (insertOrder(order)) {
						responseData = "{\"orderID\":"+order.orderID+",";
						responseData+= "\"crName\":\""+order.crName+"\",";
						responseData+= "\"crDT\":\""+order.crDT+"\"}";
						wellDone = "Y";
					}
					break;
				case "07" :	// 'Change Order' clicked				
				case "08" :	// 'Cancel Order' clicked
				case "09" :	// 'Discount' changed					
					json = (JsonObject) element;
					order = new Order();
					order = gson.fromJson(json, Order.class);
					if (updateOrder(order)) {
						responseData = "{\"taxAmt\":"+order.taxAmt+",";
						responseData+= "\"updName\":\""+order.updName+"\",";
						responseData+= "\"updDT\":\""+order.updDT+"\"}";
						wellDone = "Y";
					}
					break;
				case "12" :	// 'Item #' inserted
					int itemID = element.getAsJsonObject().get("itemID").getAsInt();
					if (readItem(itemID)) wellDone = "Y";
					break;
				case "15" :	// 'Add Item' clicked			
				case "16" :	// 'Change Item' clicked
				case "17" :	// 'Delete Item' clicked
					json = (JsonObject) element;
					orderItem = new OrderItem();
					orderItem = gson.fromJson(json, OrderItem.class);
					boolean good = whatEvent.equals("15") ? insertOrderItem(orderItem) : updateOrderItem(orderItem);
					if (good) {
						order = new Order();
						order.orderID = orderItem.orderID;
						if (updateOrder(order)) {
							summary.updName = order.updName;
							summary.updDT   = order.updDT;
							gson = new GsonBuilder().setDateFormat("yyyy-MM-dd' 'HH:mm:ss").create();			
							element = gson.toJsonTree(summary);				
							json = (JsonObject) element;
							responseData = json.toString();
							wellDone = "Y";
						}
					}
					break;
				default:	// just in case
					System.out.println("wrong whatEvent value: "+whatEvent);		        					
					break;
			}
		} catch (SQLException | JsonParseException | ParseException e) {
			e.printStackTrace();
		} finally {
			// write cookie which notes client about the server process is well done or not
			String path = request.getContextPath();		
			Cookie cookie = new Cookie("wellDone", wellDone);
			cookie.setPath(path);   // set cookie's usage range
			cookie.setMaxAge(-1);	// do not delete until browser finishes	
			response.addCookie(cookie);
			// write response data to send to client
			if (wellDone.equals("N")) responseData = "{\"Message\":\"Not done well\"}";
			System.out.println("response data is " + responseData);
			response.setContentType("json");
			PrintWriter writer = response.getWriter();
			writer.println(responseData);
			writer.flush();
			writer.close();
			// forward control to JSP (not to callback function)
//			RequestDispatcher rd = request.getRequestDispatcher("/RequestJSP.jsp");    // Path which excludes context path (start with /)
//			rd.forward(request,  response);
//			System.out.println("ending doPost()");
		}
    }
}