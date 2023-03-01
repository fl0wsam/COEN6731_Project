package io.swagger.client;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

public class SkiersServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(SkiersServlet.class.getName());
    private static final int SUCCESS = 201;
    private static final int SERVER_ERROR = 500;
    private static final int CLIENT_ERROR = 400;
    private static final String ERROR_MESSAGE = "Invalid request parameters";
    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> LIFT_RIDES =
            new ConcurrentHashMap<>();

    /**
     * Returns a JSON response with the status and message.
     *
     * @param response Http response object
     * @param status Http status code
     * @param message Http response message
     */
    private static void sendJsonResponse(HttpServletResponse response, int status, String message) {
        try {
            response.setContentType("application/json");
            response.setStatus(status);
            response.getWriter().println(new Gson().toJson(message));
        } catch (IOException e) {
            LOGGER.severe("Error sending JSON response: " + e.getMessage());
        }
    }

    /**
     * Returns a JSON response with the status and lift ride data.
     *
     * @param response Http response object
     * @param status Http status code
     * @param liftRide Lift ride data
     */
    private static void sendJsonResponse(HttpServletResponse response, int status,
                                         ConcurrentHashMap<String, Integer> liftRide) {
        try {
            response.setContentType("application/json");
            response.setStatus(status);
            response.getWriter().println(new Gson().toJson(liftRide));
        } catch (IOException e) {
            LOGGER.severe("Error sending JSON response: " + e.getMessage());
        }
    }

    /**
     * Handles HTTP POST requests containing lift ride data.
     *
     * @param request Http request object
     * @param response Http response object
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int skierId, resortId, dayId, time, liftId;
        try {
            skierId = Integer.parseInt(request.getParameter("skierId"));
            resortId = Integer.parseInt(request.getParameter("resortId"));
            dayId = Integer.parseInt(request.getParameter("dayId"));
            time = Integer.parseInt(request.getParameter("time"));
            liftId = Integer.parseInt(request.getParameter("liftId"));
        } catch (NumberFormatException | NullPointerException e) {
            LOGGER.severe("Invalid request parameters: " + e.getMessage());
            sendJsonResponse(response, CLIENT_ERROR, ERROR_MESSAGE);
            return;
        }

        if (skierId < 1 || skierId > 100000 || resortId < 1 || resortId > 10 || liftId < 1
                || liftId > 40 || dayId < 1 || time < 1 || time > 360) {
            LOGGER.severe("Invalid request parameters: " + request.getQueryString());
            sendJsonResponse(response, CLIENT_ERROR, ERROR_MESSAGE);
            return;
        }

        ConcurrentHashMap<String, Integer> liftRides =
                LIFT_RIDES.computeIfAbsent(skierId, k -> new ConcurrentHashMap<>());
        liftRides.put(dayId + ":" + time + ":" + liftId, resortId);

        sendJsonResponse(response, SUCCESS, liftRides);
    }
}
