package com.bonnier;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.ReactionAddedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.slack.api.model.block.Blocks.actions;
import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

public class AbbeSlackApp {
    public static void main(String[] args) throws Exception {
        // App expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)
        Logger logger = LoggerFactory.getLogger(AbbeSlackApp.class);
        App app = new App();

        app.command("/sake", (req, ctx) -> {
            return ctx.ack("No drinking when :skier: !");
        });

        app.blockAction("button1", (req, ctx) -> {
            String value = req.getPayload().getActions().get(0).getValue(); // "button's value"
            if (req.getPayload().getResponseUrl() != null) {
                // Post a message to the same channel if it's a block in a message
                ctx.respond("You've sent " + value + " by clicking the button!");
            }
            return ctx.ack();
        });

        app.blockAction("button2", (req, ctx) -> {
            String value = req.getPayload().getActions().get(0).getValue(); // "button's value"
            if (req.getPayload().getResponseUrl() != null) {
                // Post a message to the same channel if it's a block in a message
                ctx.respond("You've sent " + value + " by clicking the button!");
            }
            return ctx.ack();
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
                    .blocks(asBlocks(
                            section(section -> section.text(markdownText("*Please select a restaurant:*"))),
                            divider(),
                            actions(actions -> actions
                                    .elements(asElements(
                                            button(b -> b.text(plainText(pt -> pt.emoji(true).text("Farmhouse"))).actionId("button1").value("v1")),
                                            button(b -> b.text(plainText(pt -> pt.emoji(true).text("Kin Khao"))).actionId("button2").value("v2"))
                                    ))
                            )
                    )));
                    //.blocks(List.of(new DividerBlock())));
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