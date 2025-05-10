package com.example.slackbot;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import com.example.slackbot.adapters.primary.SlackEventAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import jakarta.servlet.Servlet;

@SpringBootApplication
@Import({com.example.slackbot.configuration.SecurityConfig.class, com.example.slackbot.configuration.SchedulingConfig.class})
public class SlackbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlackbotApplication.class, args);
    }

    @Bean
    public ServletRegistrationBean<Servlet> slackAppServlet(SlackEventAdapter slackEventAdapter) {
        SlackAppServlet servlet = slackEventAdapter.getServlet();
        return new ServletRegistrationBean<>(servlet, "/slack/events");
    }
} 