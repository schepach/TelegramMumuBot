package ru.mumu.bot.entity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.mumu.bot.cache.Caching;
import ru.mumu.bot.constants.Constants;
import ru.mumu.bot.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CafeMumuEntity extends AbstractCafe {

    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private static final Pattern PATTERN_MENU = Pattern.compile("([а-яА-я]*\\s?-?([а-яА-я]*)?,?){1,}(\\([а-яА-Я]*(,\\s?[а-яА-Я]*){1,}\\))?,?\\s?([а-яА-я]*\\s?-?([а-яА-я]*)?,?){1,}");
    private static final Pattern PATTERN_MENU_REPLACE = Pattern.compile("(\\([а-яА-Я]*(,\\s?[а-яА-Я]*){1,}\\))");
    private final String command;

    public CafeMumuEntity() {
        this.command = null;
    }

    public CafeMumuEntity(String command) {
        this.command = command;
    }

    @Override
    public String getMenu() {
        String lunchInfo;
        if (Caching.URL_MAP == null
                || Caching.URL_MAP.isEmpty()
                || Caching.URL_MAP.get("done") == null
                || Caching.URL_MAP.get("done").isEmpty()) {
            logger.log(Level.SEVERE, "Caching was failed...");
            return null;
        }

        // Получаем список url'ов с меню
        List<String> urlList = getUrlList(command);

        if (urlList == null || urlList.isEmpty()) {
            logger.log(Level.SEVERE, "urls is null or is empty!");
            return Constants.UNEXPECTED_ERROR;
        }

        // По этим url'ам получаем меню
        lunchInfo = getLunches(urlList);

        // Возвращаем меню
        return lunchInfo;
    }

    @Override
    public String getAddresses() {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Utils.connectToURL(Constants.ADDRESSES_URL);
            Document doc = Jsoup.connect(Constants.ADDRESSES_URL).get();
            Elements elements = doc.select("div");

            for (Element elem : elements) {

                if (elem.select("div").attr("id").equals("bp_data-39139909_120")) {

                    String text = elem.select("div").attr("id", "bp_data-39139909_120")
                            .attr("class", "bp_text").first().text();

                    text = text.substring(text.indexOf("1)"), text.lastIndexOf("contacts") + 8).trim();
                    String[] splitText = text.split("\\d\\d?\\)");

                    int count = 1;

                    for (String item : splitText) {

                        if (item.isEmpty()) continue;

                        if (count == 37) {
                            String[] lastItem = item.split("\\. ");
                            stringBuilder.append(String.valueOf(count).concat(".").concat(lastItem[0])).append("\n");
                            stringBuilder.append(lastItem[1]);
                            break;
                        }
                        stringBuilder.append(String.valueOf(count).concat(".").concat(item)).append("\n");
                        count++;
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception: ", ex);
            return Constants.UNEXPECTED_ERROR;
        }

        return Constants.ADDRESSES_TEXT.concat(stringBuilder.toString());
    }

    private List<String> getUrlList(String command) {

        List<String> urlList = new ArrayList<>();

        try {
            for (Map.Entry<String, String> entry : Caching.URL_MAP.entrySet()) {
                logger.log(Level.INFO, "check url - {0}", entry.getKey());
                if (entry.getKey().contains("ponedelnik") && command.equals(Constants.MONDAY)
                        || (entry.getKey().contains("vtornik") && command.equals(Constants.TUESDAY))
                        || (entry.getKey().contains("sreda") && command.equals(Constants.WEDNESDAY))
                        || (entry.getKey().contains("chetverg") && command.equals(Constants.THURSDAY))
                        || (entry.getKey().contains("pyatnitsa") && command.equals(Constants.FRIDAY))) {
                    logger.log(Level.INFO, "url {0} is OK, add to list...", entry.getKey());
                    urlList.add(entry.getKey());
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception from getUrlList method: ", ex);
            return null;
        }

        return urlList;
    }

    private String getLunches(List<String> urls) {
        String price = null;
        String caption = null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Constants.TIME_LUNCH.concat("\n").concat("\uD83E\uDD57\uD83C\uDF72\uD83C\uDF5D\uD83E\uDD64"));

        try {
            for (String urlWithMenu : urls) {
                logger.log(Level.INFO, "url with menu - {0}", urlWithMenu);

                String menu = null;
                Document doc = Jsoup.parse(Caching.URL_MAP.get(urlWithMenu));
                Elements elements = doc.select("body div.wrap-main.test div div.content div.food-container");
                logger.log(Level.INFO, "elements - {0}", elements);

                for (Element item : elements) {
                    // Get menu
                    menu = item.getElementsByAttributeValue("class", "info-item js-compositions").text();
                    logger.log(Level.INFO, "menu - {0}", menu);
                    // Get caption
                    caption = item.select("h1").text();
                    logger.log(Level.INFO, "caption - {0}", caption);
                    // Get price
                    price = item.select("div div.food-el-price-block div.price-block").text();
                    logger.log(Level.INFO, "price - {0}", price);
                }

                if (menu == null || menu.isEmpty()) {
                    logger.log(Level.SEVERE, "menu is null or is empty!");
                    return Constants.UNEXPECTED_ERROR;
                }

                String splitMenu = null;
                Matcher matcher = PATTERN_MENU.matcher(menu.replaceAll("\"", ""));
                // Find substring by pattern
                if (matcher.find()) {
                    splitMenu = matcher.group();
                }
                logger.log(Level.INFO, "splitMenu: {0}", splitMenu);

                if (splitMenu == null || splitMenu.isEmpty()) {
                    logger.log(Level.SEVERE, "splitMenu is null or is empty!");
                    return Constants.UNEXPECTED_ERROR;
                }

                // Replace elements from splitMenu, which match of pattern and point by empty
                splitMenu = splitMenu.replaceAll(PATTERN_MENU_REPLACE.pattern(), "").replaceAll("\\.", "");

                String[] menuItems = splitMenu.split(",");

                stringBuilder.append("\n").append(caption).append(": \n");
                int count = 1;
                for (String item : menuItems) {
                    logger.log(Level.INFO, "menuItem - {0}", item);
                    stringBuilder.append(count).append(". ").append(item.trim().concat("\n"));
                    count++;
                }
            }
            stringBuilder.insert(0, "Стоимость обеда: ".concat(price != null ? price : "").concat("\n"));

            logger.log(Level.INFO, "Final menu in mumu: {0}\n", stringBuilder.toString());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception: ", ex);
            return Constants.UNEXPECTED_ERROR;
        }

        return stringBuilder.toString();
    }

}
