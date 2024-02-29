package fib.asw.waslab02;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet(urlPatterns = {"/tweets", "/tweets/*"})
public class WoTServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private TweetDAO tweetDAO;
	private String TWEETS_URI = "/waslab02/tweets/";

    public void init() {
    	tweetDAO = new TweetDAO((java.sql.Connection) this.getServletContext().getAttribute("connection"));
    }

    @Override
	// Implements GET http://localhost:8080/waslab02/tweets
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    	response.setContentType("application/json");
		response.setHeader("Cache-control", "no-cache");
		List<Tweet> tweets= tweetDAO.getAllTweets();
		JSONArray job = new JSONArray();
		for (Tweet t: tweets) {
			JSONObject jt = new JSONObject(t);
			jt.remove("class");
			job.put(jt);
		}
		response.getWriter().println(job.toString());

    }

    @Override
	// Implements POST http://localhost:8080/waslab02/tweets/:id/likes
	//        and POST http://localhost:8080/waslab02/tweets
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String uri = request.getRequestURI();
		int lastIndex = uri.lastIndexOf("/likes");
		if (lastIndex > -1) {  // uri ends with "/likes"
			// Implements POST http://localhost:8080/waslab02/tweets/:id/likes
			long id = Long.valueOf(uri.substring(TWEETS_URI.length(),lastIndex));		
			response.setContentType("text/plain");
			response.getWriter().println(tweetDAO.likeTweet(id));
		}
		else { 
			// Implements POST http://localhost:8080/waslab02/tweets
			int max_length_of_data = request.getContentLength();
			byte[] httpInData = new byte[max_length_of_data];
			ServletInputStream  httpIn  = request.getInputStream();
			httpIn.readLine(httpInData, 0, max_length_of_data);
			String body = new String(httpInData);
			JSONObject myString = new JSONObject(body);
			String author = myString.getString("author");
			String text = myString.getString("text");
			Tweet t = tweetDAO.insertTweet(author, text);
			JSONObject tw = new JSONObject(t);
			tw.put("token", getMD5Hash(String.valueOf(t.getId())));
			response.getWriter().println(tw.toString());
			/*      ^
		      The String variable body contains the sent (JSON) Data. 
		      Complete the implementation below.*/
			
		}
	}
    
    public static String getMD5Hash(String input){
        try {
            // Obtenemos una instancia de MessageDigest para MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Actualizamos el digest con los bytes de la cadena de entrada
            md.update(input.getBytes());

            // Obtenemos el valor del hash en bytes
            byte[] digest = md.digest();

            // Convertimos los bytes del hash a una representación hexadecimal
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
	// Implements DELETE http://localhost:8080/waslab02/tweets/:id
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    	String uri = req.getRequestURI();
    	long id = Long.valueOf(uri.substring(TWEETS_URI.length()));		
    	String token = req.getHeader("Authorization");
    	String hash = getMD5Hash(String.valueOf(id));
		if(token.equals(hash)) tweetDAO.deleteTweet(id);
		
	}
}