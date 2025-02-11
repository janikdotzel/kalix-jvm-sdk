package com.example;
import kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport;

// tag::class[]
import kalix.javasdk.testkit.KalixTestKit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class TestkitConfig {
    // end::class[]

    @Profile("with-acls")
    // tag::acls[]
    @Bean
    public KalixTestKit.Settings settings() {
        return KalixTestKit.Settings.DEFAULT.withAclEnabled(); // <1>
    }
    // end::acls[]

    @Profile("with-pubsub")
    // tag::pubsub[]
    @Bean
    public KalixTestKit.Settings settingsWithPubSub() {
        return KalixTestKit.Settings.DEFAULT.withAclEnabled()
            .withEventingSupport(EventingSupport.GOOGLE_PUBSUB);
    }
    // end::pubsub[]

// tag::class[]
}
// end::class[]
