import java.sql.*;
import java.util.Scanner;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

class MySQL {
    private String host;
    private String port;
    private String password;
    private String userName;
    public Connection connection;

    public MySQL(String host, String port, String password, String userName) throws Exception {
        this.host = host;
        this.port = port;
        this.password = password;
        this.userName = userName;
        connect();
        databaseInitialize();
    }

    public void connect() throws Exception {
        String url = "jdbc:mysql://" + host + ":" + port + "?useSSL=true";

        try {   
            connection = DriverManager.getConnection(url, userName, password);
        } catch (Exception e) {
            throw new Exception(e.getMessage() + "\n" + "Error When attempting to connect to database! Credentials:\n"
                    + "URL: " + url + "\n" + userName + ":" + password);
        }

    }
    
    public  void databaseInitialize() throws Exception {
        try{
            connection.createStatement().executeUpdate("create database if not exists magazin");
            connection.createStatement().executeUpdate("use magazin");

            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS products ( date varchar(30), region varchar(30), product varchar(30), qty int, cost  float, amt float, tax float, total float)");
        }catch(Exception e){
            System.out.println("Unable to initialize database. When creating database or the table.");
            System.out.println(e.getMessage());
        }
    }
    public  String insertOneAndGetQuery() throws Exception {

          //¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨\\
         //  READING VALUES FROM THE USER  \\
        //__________________________________\\

        Scanner scanner = new Scanner(System.in);
        String date;
        String region;
        String product;
        int qty;
        double cost;
        double amt;
        double tax;
        System.out.print("Date: ");
        date = scanner.next();
        System.out.print("Product: ");
        product = scanner.next();
        System.out.print("Region: ");
        region = scanner.next();
        System.out.print("Qty: ");
        qty = Integer.parseInt(scanner.next());
        System.out.print("Cost: ");
        cost = Double.parseDouble(scanner.next());
        System.out.print("Amount: ");
        amt = Double.parseDouble(scanner.next());
        System.out.print("Tax: ");
        tax = Double.parseDouble(scanner.next());

          //¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨¨\\
         //      BUILDING THE QUERY        \\
        //__________________________________\\

        PreparedStatement preparedStatement = connection
                .prepareStatement("INSERT INTO products values ( ?, ? ,? , ? , ? , ? , ?, ?)");
        preparedStatement.setString(1, date);
        preparedStatement.setString(2, region);
        preparedStatement.setString(3, product);
        preparedStatement.setString(4, qty + "");
        preparedStatement.setString(5, cost + "");
        preparedStatement.setString(6, amt + "");
        preparedStatement.setString(7, tax + "");
        preparedStatement.setString(8, (cost + amt + tax) + "");
        
        int rowsAffected = preparedStatement.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("Row inserted successfully");
            String queryStirng = preparedStatement.toString();
            queryStirng = queryStirng.substring(queryStirng.indexOf("INSERT"));
            return queryStirng;
        } else {
            System.out.println("Failed to insert row");
            return null;
        }

    }
    public  void fetchAll() throws SQLException {
        System.out.println("once in.?");
        PreparedStatement pst = connection.prepareStatement("SELECT * FROM products");
        ResultSet resultSet = pst.executeQuery();
        int columnCount = resultSet.getMetaData().getColumnCount();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int nbr=1;
        System.out.println("columncount"+columnCount);
        System.out.println("never out?");

        while (resultSet.next()) {
            System.out.println("Proudct N°"+nbr+" ------------- ");
            nbr++;
            for (int i = 1; i <= columnCount; i++) {
                System.out.print("    "+ metaData.getColumnName(i) + ": ");
                System.out.println(resultSet.getString(i));
            }
            System.out.println();
        }
    }
}

class RabbitMQ{
    private String host;
    private String port;
    private String queueName;
    public com.rabbitmq.client.Connection connection;
    public Channel channel;

    public RabbitMQ(String host, String port, String queueName) throws Exception {
        this.host = host;
        this.port = port;
        this.queueName = queueName;
        connect();
        initialize();
    }

    public void connect() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(Integer.parseInt(port));
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (Exception e) {
            throw new Exception(e.getMessage() + "\n" + "Error When attempting to connect to RabbitMQ! Credentials:\n"
                    + "Host: " + host + "\n" + "Port: " + port);
        }
    }

    public void initialize() throws Exception {
        try {
            channel.queueDeclare(queueName, false, false, false, null);
        } catch (Exception e) {
            throw new Exception(e.getMessage() + "\n" + "Error When attempting to initialize RabbitMQ! Credentials:\n"
                    + "Queue Name: " + queueName);
        }
    }
    /**
     * sends the query to the RabbitMQ server to be synced with the other databases 
     * @param query
     * @throws Exception
     */
    public void sync(String query) throws Exception{
        channel.basicPublish("", queueName, null, query.getBytes());
    }    
    
       
    


}

public class App {
    // default values
    private static String DB_HOST = "172.17.0.1";
    private static String DB_PORT = "3306";
    private static String DB_PASSWORD = "kool";
    private static String DB_USER_NAME = "root";

    private static String RABBIT_HOST = "172.17.0.1";
    private static String RABBIT_PORT = "5672";
    
    private static String QUEUE_NAME = "synq";


    public static void readEnvironement(){
        
        DB_HOST=System.getProperty("DB_HOST", DB_HOST);
        DB_PORT=System.getProperty("DB_PORT", DB_PORT);
        DB_PASSWORD=System.getProperty("DB_PASSWORD", DB_PASSWORD);
        DB_USER_NAME=System.getProperty("DB_USER_NAME", DB_USER_NAME);
        RABBIT_HOST=System.getProperty("RABBIT_HOST", RABBIT_HOST);
        RABBIT_PORT=System.getProperty("RABBIT_PORT", RABBIT_PORT);
        System.out.println("variable environment status");
        System.out.println("\n----------------------------------------------------\n");
        if (System.getProperty("DB_HOST") == null)
            System.out.println("DB_HOST is not set. Using default value: " + DB_HOST);
        if (System.getProperty("DB_PORT") == null)
            System.out.println("DB_PORT is not set. Using default value: " + DB_PORT);
        if (System.getProperty("DB_PASSWORD") == null)
            System.out.println("DB_PASSWORD is not set. Using default value: " + DB_PASSWORD);
        if (System.getProperty("DB_USER_NAME") == null)
            System.out.println("DB_USER_NAME is not set. Using default value: " + DB_USER_NAME);
        if (System.getProperty("RABBIT_HOST") == null)
            System.out.println("RABBIT_HOST is not set. Using default value: " + RABBIT_HOST);
        if (System.getProperty("RABBIT_PORT") == null)
            System.out.println("RABBIT_PORT is not set. Using default value: " + RABBIT_PORT);
        System.out.println("\n----------------------------------------------------\n");
    }

 
    public static void main(String args[]) throws Exception {
        readEnvironement();
        MySQL MySQL = new MySQL(DB_HOST, DB_PORT, DB_PASSWORD, DB_USER_NAME);
        RabbitMQ RabbitMQ = new RabbitMQ(RABBIT_HOST, RABBIT_PORT, QUEUE_NAME);              
        while (true) {
            System.out.println("1 - Add new Product");
            System.out.println("2 - Get All Products");
            System.out.print("Your choice : ");
            Scanner scanner = new Scanner(System.in);
            String choice="";
            scanner.reset();
            choice = scanner.nextLine();
                
            System.out.println(choice);
            switch (choice) {
                case "1":
                    String query;
                    try {
                        query = MySQL.insertOneAndGetQuery();
                        try{
                            if (query != null)
                                RabbitMQ.sync(query);
                        }catch(Exception e){
                            System.out.println("Failed to sync! Unable to communicate with the RabbitMQ server");
                            System.out.println(e.getMessage());
                        }

                    } catch (Exception e) {
                        System.out.println("Invalid input ");
                        System.out.println(e.getMessage());
                    }
                    
                    break;

                case "2":
                    MySQL.fetchAll();
                    break;
                default:
                    System.out.println("Invalid Choice!");
            }

        }

    }
}
