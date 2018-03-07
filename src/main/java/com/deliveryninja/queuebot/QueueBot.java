package com.deliveryninja.queuebot;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.Controller;
import me.ramswaroop.jbot.core.slack.EventType;
import me.ramswaroop.jbot.core.slack.models.Event;
import me.ramswaroop.jbot.core.slack.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.regex.Matcher;

@Component
public class QueueBot extends Bot {

    private static final Logger logger = LoggerFactory.getLogger(QueueBot.class);

    private Deque<User> userQueue = new LinkedList();

    private User currentUser = new User();

    @Value("${slackBotToken}")
    private String slackToken;

    @Override
    public String getSlackToken() {
        return slackToken;
    }

    @Override
    public Bot getSlackBot() {
        return this;
    }

    @Controller(pattern = "(show queue)")
    public void showQueue(WebSocketSession session, Event event){
        reply(session, event, new Message(getCurrentQueue()));
    }

    @Controller(pattern = "(leave queue)")
    public void leaveQueue(WebSocketSession session, Event event){
        currentUser = new User();
        currentUser.setUserId(getSlackUsername(event));

        userQueue.remove(currentUser);

        reply(session, event, new Message("You have been removed from the queue"));
        reply(session, event, new Message(getCurrentQueue()));
    }

    @Controller(pattern = "(join queue)", next = "setDescription")
    public void joinQueue(WebSocketSession session, Event event) {
        logger.debug("Joining the queue");

        getSlackUsername(event);
        currentUser = new User();
        currentUser.setUserId(getSlackUsername(event));

        if(userQueue.contains(currentUser)){
            reply(session, event,
                    new Message("You already have a position in the queue please leave the queue to add a new job"));
        } else {
            startConversation(event, "setDescription");   // start conversation
            reply(session, event, new Message("✒️ Please enter a description for your merge requests"));
        }
    }

    @Controller(pattern = "(XXXXX)", next = "confirm")
    public void setDescription(WebSocketSession session, Event event){
        logger.debug("Setting description");

        currentUser.setMessage(event.getText());

        reply(session, event, new Message(getCurrentQueue()));
        reply(session, event, new Message("✅ Confirm yes if you would like to join the queue"));
        nextConversation(event);
    }

    @Controller
    public void confirm(WebSocketSession session, Event event){
        logger.debug("Confirming adding to the queue");

        logger.debug("Event text: " + event.getText());
        if(event.getText().contains("yes")) {
            reply(session, event, new Message("\uD83D\uDC4C You have been successfully added to the queue in position : " + (userQueue.size() + 1)));
            userQueue.add(currentUser);
        } else {
            reply(session, event, new Message("\uD83D\uDE05 Bye!"));
        }
        stopConversation(event);
    }

    private String getCurrentQueue(){
        StringBuilder message = new StringBuilder();
        message.append("*CURRENT QUEUE*\n");
        message.append("\n");

        int i = 1;
        for(User user : userQueue){
            message.append("*" + i + ". " + user.getUserId() + "*\n");
            message.append("    " + user.getMessage() + "\n");
            message.append("\n");
            i++;
        }

        return message.toString();
    }

    public String getSlackUsername(Event event) {
        Slack slack = Slack.getInstance();
        try {
            UsersInfoResponse usersInfoResponse = slack.methods().usersInfo(UsersInfoRequest.builder().token(slackToken).user(event.getUserId()).build());
            return usersInfoResponse.getUser().getName();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SlackApiException e) {
            e.printStackTrace();
        }
        return "N/A";
    }
}
