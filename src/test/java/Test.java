
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.telegram.api.message.TLMessage;


/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief TODO
 * @date 16 of October of 2016
 */
public class Test {
  private static final int APIKEY = 59204; // your api key
  private static final String APIHASH = "750a9e3a38b8f12266ec95aec9ec4cc8"; // your api hash
  private static final String PHONENUMBER = "+40755134431"; // Your phone number

  public static void main(String[] args) throws IOException, TimeoutException, TelegramException {
    TelegramApiHelper apiHelper = new TelegramApiHelper(APIKEY, APIHASH, PHONENUMBER, new ConfirmationCodeReceiver() {
      @Override
      public String getConfirmationCode() {
        String code = null;
        JFrame jframe = new JFrame("Code");
        try {
          code = JOptionPane.showInputDialog("Code");
        } finally {
          jframe.dispatchEvent(new WindowEvent(jframe, WindowEvent.WINDOW_CLOSING));
        }
        return code;
      }
    });

    apiHelper.login();

    System.err.println("***************");
    // int i = 1;
    // for (TLMessage m : apiHelper.getChanellMessages(new Date(System.currentTimeMillis() -
    // TimeUnit.DAYS.toMillis(30)),
    // "Be Like Beard")) {
    // System.out.print(
    // (i++) + " " + new Date(m.getDate() * 1000L));
    // if (m.getMedia()!=null){
    // System.out.println(m.getMedia().getClass());
    // }else{
    // System.out.println();
    // }
    // }
    int i = 1;
    for (TLMessage m : apiHelper.getChanellMessages(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)),
        "Tech Experts")) {
      System.out.print((i++) + " " + new Date(m.getDate() * 1000L));
      System.out.println(" " + m.getFromId() + " " + m.getMessage());
    }

    apiHelper.stop();
    System.out.println();
  }

}
