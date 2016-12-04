package ru.mumu.utils.helper;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.objects.Message;
import ru.mumu.connection.Connection;
import ru.mumu.constants.Constants;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alexeyoblomov on 03.12.16.
 */
public class BotHelper {

    private static final Logger LOGGER = Logger.getLogger(BotHelper.class.getSimpleName());

    public static String checkMessage(String message, Date currentDate, String messageDay, String currentDay) {

        try {

            switch (message) {
                case Constants.HELP:
                    LOGGER.info("TextForUser: " + Constants.HELP_TEXT);
                    return Constants.HELP_TEXT;
                case Constants.START:
                    LOGGER.info("TextForUser: " + Constants.START_TEXT);
                    return Constants.START_TEXT;
                case Constants.MONDAY:
                    return Connection.sendRequest(currentDate, Constants.MONDAY);
                case Constants.TUESDAY:
                    return Connection.sendRequest(currentDate, Constants.TUESDAY);
                case Constants.WEDNESDAY:
                    return Connection.sendRequest(currentDate, Constants.WEDNESDAY);
                case Constants.THURSDAY:
                    return Connection.sendRequest(currentDate, Constants.THURSDAY);
                case Constants.FRIDAY:
                    return Connection.sendRequest(currentDate, Constants.FRIDAY);
                case Constants.VICTORIA:
                    return Connection.sendRequest(Constants.VICTORIA);
                case Constants.ADDRESSES:
                    return Connection.sendRequest(Constants.ADDRESSES);
                case Constants.TODAY:
                    return checkCommandToday(currentDay, messageDay, currentDate);
                default:
                    LOGGER.info("TextForUser: " + Constants.ERROR_OTHER_INPUT);
                    return Constants.ERROR_OTHER_INPUT;
            }
        } catch (IOException e) {
            LOGGER.error(Constants.UNEXPECTED_ERROR.concat(e.getMessage()) + e);
        }

        return Constants.BAD_COMMAND;
    }


    public static String getTextForUser(Message message, String text) {

        String userName = "";
        String textForUser;

        if (message.getFrom().getFirstName() != null && !message.getFrom().getFirstName().isEmpty()) {
            userName = message.getFrom().getFirstName();
        } else if (message.getFrom().getUserName() != null && !message.getFrom().getUserName().isEmpty()) {
            userName = message.getFrom().getUserName();
        } else if (message.getFrom().getLastName() != null && !message.getFrom().getLastName().isEmpty()) {
            userName = message.getFrom().getLastName();
        }

        switch (text) {
            case Constants.ERROR_HOLIDAY_DAY:
            case Constants.ERROR_OTHER_INPUT:
            case Constants.HELP_TEXT:
                LOGGER.info("TextForUser: " + text);
                textForUser = userName.concat(", ").concat(text);
                break;
            case Constants.START_TEXT:
                textForUser = "Hey, ".concat(userName.concat(", ").concat(text));
                break;
            default:
                textForUser = "\uD83D\uDCE2 " + userName.concat(", ").concat(Constants.CAFE_MUMU_TEXT).concat(text);
                break;
        }

        if (text.contains(Constants.ADDRESSES_TEXT) || text.contains(Constants.VICTORIA_TEXT)) {
            textForUser = "\uD83D\uDCE2 " + userName.concat(", ").concat(text);
        }

        return textForUser;
    }

    public static boolean checkDayOfMonth(String groupInfo, Date dateCurrent) {
        LOGGER.info("Check groupInfo = " + groupInfo);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String[] arrGroupInfo = groupInfo.split("");
        String dateStart = arrGroupInfo[6] + arrGroupInfo[7] + "/" + arrGroupInfo[9] + arrGroupInfo[10] + "/" + currentYear;
        String dateEnd = arrGroupInfo[13] + arrGroupInfo[14] + "/" + arrGroupInfo[16] + arrGroupInfo[17] + "/" + currentYear;

        Date startDate = convertStringToDate(dateStart);
        Date endDate = convertStringToDate(dateEnd);

        return dateCurrent.after(startDate) && dateCurrent.before(endDate) || dateCurrent.equals(endDate);
    }

    private static Date convertStringToDate(String strDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = null;

        try {
            date = formatter.parse(strDate);
            LOGGER.info("Date from converter: " + date);

        } catch (ParseException e) {
            LOGGER.error(e.getMessage() + e);
        }
        return date;
    }

    private static String checkCommandToday(String currentDay, String messageDay, Date dateCurrent) throws IOException {
        LOGGER.info("currentDay:  " + currentDay);
        LOGGER.info("messageDay:  " + messageDay);

        if (currentDay.toLowerCase().equals("saturday") || currentDay.toLowerCase().equals("sunday")) {
            return Constants.ERROR_HOLIDAY_DAY;
        } else {
            if (currentDay.equals(messageDay)) {
                String weekDay = "/".concat(currentDay).toLowerCase();
                LOGGER.info("WeekDay is : " + weekDay);
                return Connection.sendRequest(dateCurrent, weekDay);
            } else {
                return "Days are not equals!";
            }
        }
    }

    public static boolean checkString(String str) {
        Pattern p = Pattern.compile("горошек|кукуруза\\)");
        Matcher m = p.matcher(str);
        return m.matches();
    }
}
