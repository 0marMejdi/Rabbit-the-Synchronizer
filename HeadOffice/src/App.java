import java.sql.*;
import java.util.Scanner;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Channel;

class App {
    private static Connection __connection = null;

    public static void fetchAll() throws Exception {

        Connection conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("SELECT * FROM products");
        ResultSet resulta = pst.executeQuery();
        printRow(resulta);
    }

    public static void sync() throws Exception {
        String host = "localhost";
        int port1 = 5673;
        int port2 = 5674;
        if (System.getProperty("RABBIT_PORT1") != null)
            port1 = Integer.parseInt(System.getProperty("RABBIT_PORT1"));
        if (System.getProperty("RABBIT_PORT2") != null)
            port2 = Integer.parseInt(System.getProperty("RABBIT_PORT2"));
        if (System.getProperty("RABBIT_HOST") != null)
            host = System.getProperty("RABBIT_HOST");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setPort(port1);
        com.rabbitmq.client.Connection connection1 = factory.newConnection();
        factory.setPort(port2);
        com.rabbitmq.client.Connection connection2 = factory.newConnection();
        Channel channel1 = connection1.createChannel();
        Channel channel2 = connection2.createChannel();
        channel1.queueDeclare("synq", false, false, true, null);
        channel2.queueDeclare("synq", false, false, true, null);
        DeliverCallback deliverCallback = (consumerTag, message) -> {
            String syncQuery = new String(message.getBody(), "UTF-8") ;
            syncQuery=syncQuery.substring(syncQuery.indexOf("INSERT"));
            try {
                PreparedStatement pStatement = getConnection().prepareStatement(syncQuery);
                pStatement.executeUpdate();
                System.out.println("____\n\nSync database:\n  executing following query: " + syncQuery);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };
        channel1.basicConsume("synq", deliverCallback,(customerTag)->{});
        channel2.basicConsume("synq", deliverCallback,(customerTag)->{});
        

    }

    /**
     * connects to the database, checks the existance of table (creates one if not
     * found), and returns the instance of the connection
     * port, host, user name and password are retrieved from variable environements,
     * if not passed (value is null) it will use the default credentials.$
     * 
     * @return a Connection instance
     * @throws Exception
     */
    public static Connection getConnection() throws Exception {
        if (__connection != null)
            return __connection;
        // Default port is 3306 , otherwise DB_PORT
        String port = System.getProperty("DB_PORT") != null ? System.getProperty("DB_PORT") : "3307";
        // Default host is localhost, otherwise DB_HOST
        String host = System.getProperty("DB_HOST") != null ? System.getProperty("DB_HOST") : "localhost";
        // The final URL
        String url = "jdbc:mysql://" + host + ":" + port + "?useSSL=true";
        // Default username is root, otherwise DB_USER_NAME
        String user = System.getProperty("DB_USENAME") != null ? System.getProperty("DB_USER_NAME") : "root";
        // Default username is kool, otherwise DB_PASSWORD
        String password = System.getProperty("DB_PASSWORD") != null ? System.getProperty("DB_PASSWORD") : "kool";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            conn.createStatement().executeUpdate("create database if not exists magazin");
            conn.createStatement().executeUpdate("use magazin");

            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS products ( date varchar(30), region varchar(30), product varchar(30), qty int, cost  float, amt float, tax float, total float)");
            __connection = conn;
            System.out.println("Successfully connected to database! Credentials:\n"
                    + "URL: " + url + "\n" + user + ":" + password);
            return conn;

        } catch (Exception e) {
            throw new Exception(e.getMessage() + "\n" + "Error When attempting to connect to database! Credentials:\n"
                    + "URL: " + url + "\n" + user + ":" + password);

        }

    }

    /**
     * inserts a row into database with the given informations in the params.
     */

    public static void insertOne(String date, String region, String product, int qty, double cost, double amt,
            double tax) throws Exception {
        Connection conn = getConnection();
        PreparedStatement preparedStatement = conn
                .prepareStatement("INSERT INTO products values ( ?, ? ,? , ? , ? , ? , ?, ?)");
        preparedStatement.setString(1, date);
        preparedStatement.setString(2, region);
        preparedStatement.setString(3, product);
        preparedStatement.setString(4, qty + "");
        preparedStatement.setString(5, cost + "");
        preparedStatement.setString(6, amt + "");
        preparedStatement.setString(7, tax + "");
        preparedStatement.setString(8, (cost + amt + tax) + "");
        System.out.println("preparedStatement.toString() = " + preparedStatement.toString());
        int rowsAffected = preparedStatement.executeUpdate();
        // ! : Here when the synchronisation happens
        //sync(preparedStatement.toString());

    }

    /**
     * reads from the user all the fields, gathers the necessary arguments to insert
     * them into the database.
     * 
     * @throws Exception
     */
    public static void readRow() throws Exception {
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
        insertOne(date, region, product, qty, cost, amt, tax);
        System.out.println("Row Affected Successfully");

    }

    public static void printRow(ResultSet resultSet) throws SQLException {
        int columnCount = resultSet.getMetaData().getColumnCount();
        ResultSetMetaData metaData = resultSet.getMetaData();
        // Iterate over column names and print them separated by spaces
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(metaData.getColumnName(i) + " ");
        }
        System.out.println();
        // Iterate over the result set
        while (resultSet.next()) {
            // Print each cell value separated by spaces
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(resultSet.getString(i) + " ");
            }
            // Move to the next line after printing all cell values for the row
            System.out.println();
        }
    }

    public static void main(String args[]) throws Exception {

        Connection conn = getConnection();
        while (true) {
            System.out.println("1 - Add new Product");
            System.out.println("2 - Get All Products");
            System.out.print("Your choice : ");
            Scanner scanner = new Scanner(System.in);
            String choice;
            sync();
            if (scanner.hasNextLine())
                choice = scanner.nextLine();
            else {
                scanner.close();
                return;
            }
            switch (choice) {
                case "1":
                    try {
                        readRow();

                    } catch (Exception e) {
                        System.out.println("Invalid input ");
                        System.out.println(e.getMessage());
                    }
                    break;

                case "2":
                    fetchAll();
                    break;
                default:
                    System.out.println("Invalid Choice!");
            }

        }

    }
}
