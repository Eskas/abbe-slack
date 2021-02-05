package com.bonnier;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.request.auth.AuthTestRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.RichTextBlock;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.ReactionAddedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AbbeSlackApp {
    public static void main(String[] args) throws Exception {
        // App expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)
        Logger logger = LoggerFactory.getLogger(AbbeSlackApp.class);
        App app = new App();

        app.command("/sake", (req, ctx) -> {
            return ctx.ack("No drinking when :skier: !");
        });

        app.event(MessageEvent.class, ((messagePayload, eventContext) -> {
            String text = messagePayload.getEvent().getText();
            logger.info("Token: " + messagePayload.getToken());
            logger.info("Channel: " + messagePayload.getEvent().getChannel());
            logger.info("TS: " + messagePayload.getEvent().getTs());
            //eventContext.client().authTest(a -> a.token(messagePayload.getToken()));
            final ChatUpdateResponse r = eventContext.client().chatUpdate(u ->
                    u.channel(messagePayload.getEvent().getChannel())
                            .text(messagePayload.getEvent().getText() + " och lite mer text!")
                            .token(System.getenv("SLACK_BOT_TOKEN"))
                            .user(messagePayload.getEvent().getUser())
                            .ts(messagePayload.getEvent().getTs())
            );
            logger.info(r.getError());
            ChatPostMessageResponse message = eventContext.client().chatPostMessage(rs -> rs
                    .channel(messagePayload.getEvent().getChannel())
                    .threadTs(messagePayload.getEvent().getTs())
                    .blocks(List.of(new DividerBlock())));
                    //.text("Här är ett svar!"));
            logger.info(message.getError());
            logger.info("MessageText = " + text);
            logger.info(messagePayload.getEvent().getChannel());
            return eventContext.ack();
        }));

        app.event(ReactionAddedEvent.class, (payload, ctx) -> {
            ReactionAddedEvent event = payload.getEvent();
            if (event.getReaction().equals("skier")) {
                ChatPostMessageResponse message = ctx.client().chatPostMessage(r -> r
                        .channel(event.getItem().getChannel())
                        .threadTs(event.getItem().getTs())
                        .text("<@" + event.getUser() + "> gonna ski some deeeeep powder!"));
                if (!message.isOk()) {
                    ctx.logger.error("chat.postMessage failed: {}", message.getError());
                }
            }
            return ctx.ack();
        });

        SlackAppServer server = new SlackAppServer(app);
        server.start(); // http://localhost:3000/slack/events
    }
}