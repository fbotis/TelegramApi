
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.telegram.api.TLConfig;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.auth.TLSentCode;
import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.functions.auth.TLRequestAuthSendCode;
import org.telegram.api.functions.auth.TLRequestAuthSignIn;
import org.telegram.api.functions.help.TLRequestHelpGetConfig;
import org.telegram.api.functions.messages.TLRequestMessagesGetDialogs;
import org.telegram.api.functions.messages.TLRequestMessagesGetHistory;
import org.telegram.api.functions.updates.TLRequestUpdatesGetState;
import org.telegram.api.input.peer.TLInputPeerChannel;
import org.telegram.api.input.peer.TLInputPeerSelf;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.message.TLMessage;
import org.telegram.api.messages.dialogs.TLAbsDialogs;
import org.telegram.api.updates.TLAbsUpdates;
import org.telegram.api.updates.TLUpdatesState;
import org.telegram.util.MemoryApiState;

public class TelegramApiHelper {
  private int appId;
  private String apiHash;
  private String phoneNumber;
  private AppInfo appInfo;
  private MemoryApiState apiState;
  private TelegramApi api;
  private ConfirmationCodeReceiver confirmationCodeReceiver;

  public TelegramApiHelper(int appId, String apiHash, String phoneNumber,
      ConfirmationCodeReceiver confirmationCodeReceiver) {
    this.appId = appId;
    this.apiHash = apiHash;
    this.phoneNumber = phoneNumber;
    this.appInfo = new AppInfo(appId, "Myapp", "154", "587", "en");
    this.apiState = new MemoryApiState("apistatee.bin");
    this.confirmationCodeReceiver = confirmationCodeReceiver;
  }

  public void login() throws TelegramException {
    try {
      this.api = new TelegramApi(apiState, appInfo, new ApiCallback() {

        @Override
        public void onUpdatesInvalidated(TelegramApi api) {}

        @Override
        public void onUpdate(TLAbsUpdates var1) {}

        @Override
        public void onAuthCancelled(TelegramApi var1) {

        }
      });

      updateApiStateSettings();

      if (!api.getState().isAuthenticated()) {
        signIn();
      }
    } catch (Exception ex) {
      throw new TelegramException(ex);
    }
  }

  private void updateApiStateSettings() throws RpcException, TimeoutException {
    TLConfig config = api.doRpcCallNonAuth(new TLRequestHelpGetConfig());
    apiState.updateSettings(config);
  }

  private void signIn() throws TimeoutException, IOException {
    TLSentCode sentCode;
    TLRequestAuthSendCode s = new TLRequestAuthSendCode();
    try {
      s.setPhoneNumber(phoneNumber);
      s.setApiHash(apiHash);
      s.setApiId(appId);
      sentCode = api.doRpcCallNonAuth(s);
    } catch (RpcException e) {
      if (e.getErrorCode() == 303) {
        int destDC;
        if (e.getErrorTag().startsWith("NETWORK_MIGRATE_")) {
          destDC = Integer.parseInt(e.getErrorTag().substring("NETWORK_MIGRATE_".length()));
        } else if (e.getErrorTag().startsWith("PHONE_MIGRATE_")) {
          destDC = Integer.parseInt(e.getErrorTag().substring("PHONE_MIGRATE_".length()));
        } else if (e.getErrorTag().startsWith("USER_MIGRATE_")) {
          destDC = Integer.parseInt(e.getErrorTag().substring("USER_MIGRATE_".length()));
        } else {
          throw e;
        }
        api.switchToDc(destDC);
        sentCode = api.doRpcCallNonAuth(s);
      } else {
        throw e;
      }
    }

    String code = confirmationCodeReceiver.getConfirmationCode();
    TLRequestAuthSignIn signIn = new TLRequestAuthSignIn();
    signIn.setPhoneCode(code);
    signIn.setPhoneCodeHash(sentCode.getPhoneCodeHash());
    signIn.setPhoneNumber(phoneNumber);
    TLAuthorization auth = api.doRpcCallNonAuth(signIn);
    apiState.setAuthenticated(apiState.getPrimaryDc(), true);
    TLUpdatesState state = (TLUpdatesState) api.doRpcCall(new TLRequestUpdatesGetState());
  }


  public List<TLMessage> getChanellMessages(Date lastDate, String channelName) throws TelegramException {
    ArrayList<TLMessage> messages = new ArrayList<>();
    try {
      TLRequestMessagesGetDialogs getDialogs = new TLRequestMessagesGetDialogs();
      // getDialogs.setOffsetDate((int) lastDate.getTime() / 1000);
      // getDialogs.setLimit(10000);
      getDialogs.setOffsetPeer(new TLInputPeerSelf());;
      TLAbsDialogs dialogs = api.doRpcCall(getDialogs);
      for (TLAbsChat c : dialogs.getChats()) {
        if (c.getClass().equals(TLChannel.class) && ((TLChannel) c).getTitle().equals(channelName)) {
          TLChannel cc = (TLChannel) c;
          TLMessage lastMsg = null;
          messages.addAll(getMessages(cc, lastDate, lastMsg));
        }
      }
      return messages;
    } catch (Exception ex) {
      throw new TelegramException(ex);
    }
  }

  private Collection<? extends TLMessage> getMessages(TLChannel cc, Date lastDate, TLMessage lastMsg)
      throws IOException, TimeoutException {
    return getMessages(cc, (int) (lastDate.getTime() / 1000), lastMsg, new ArrayList<TLMessage>());
  }

  private ArrayList<TLMessage> getMessages(TLChannel cc, int lastDate, TLMessage lastMsg,
      ArrayList<TLMessage> accumulator) throws IOException, TimeoutException {
    TLRequestMessagesGetHistory getHist = new TLRequestMessagesGetHistory();
    TLInputPeerChannel ch = new TLInputPeerChannel();
    ch.setChannelId(cc.getId());
    ch.setAccessHash(cc.getAccessHash());
    getHist.setPeer(ch);
    getHist.setLimit(100);;
    if (lastMsg != null) {
      getHist.setOffsetId(lastMsg.getId());
    }
    TLMessage lastTlMessage = null;
    for (TLAbsMessage m : api.doRpcCall(getHist).getMessages()) {
      if (!(m instanceof TLMessage))
        continue;
      lastTlMessage = (TLMessage) m;
      if (lastDate < lastTlMessage.getDate()) {
        accumulator.add(lastTlMessage);
      } else {
        return accumulator;
      }

    }
    return getMessages(cc, lastDate, lastTlMessage, accumulator);
  }

  public void stop() {
    api.close();
  }



}
