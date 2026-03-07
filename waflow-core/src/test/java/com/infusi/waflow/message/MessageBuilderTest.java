package com.infusi.waflow.message;

import com.infusi.waflow.message.builder.MessageBuilder;
import com.infusi.waflow.message.types.ButtonMessage;
import com.infusi.waflow.message.types.ListMessage;
import com.infusi.waflow.message.types.TextMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageBuilderTest {

    private static final String PHONE = "233201234567";

    @Nested
    @DisplayName("TextMessage")
    class TextTests {

        @Test
        @DisplayName("builds a simple text message")
        void shouldBuildTextMessage() {
            TextMessage msg = MessageBuilder.to(PHONE).text("Hello!");

            assertThat(msg.getTo()).isEqualTo(PHONE);
            assertThat(msg.getBody()).isEqualTo("Hello!");
            assertThat(msg.isPreviewUrl()).isFalse();
        }

        @Test
        @DisplayName("builds text message with preview URL")
        void shouldBuildTextWithPreviewUrl() {
            TextMessage msg = MessageBuilder.to(PHONE).text("Check this out", true);

            assertThat(msg.isPreviewUrl()).isTrue();
        }
    }

    @Nested
    @DisplayName("ListMessage")
    class ListTests {

        @Test
        @DisplayName("builds a list message with sections and rows")
        void shouldBuildListMessage() {
            ListMessage msg = MessageBuilder.to(PHONE)
                    .list()
                    .header("Services")
                    .body("Pick a service")
                    .footer("Powered by WaFlow")
                    .buttonText("View")
                    .section("Makeup", s -> {
                        s.row("bridal", "Bridal Makeup", "GHS 500");
                        s.row("party", "Party Makeup", "GHS 200");
                    })
                    .section("Hair", s -> s.row("braids", "Braids", "GHS 150"))
                    .build();

            assertThat(msg.getTo()).isEqualTo(PHONE);
            assertThat(msg.getHeader()).isEqualTo("Services");
            assertThat(msg.getBody()).isEqualTo("Pick a service");
            assertThat(msg.getFooter()).isEqualTo("Powered by WaFlow");
            assertThat(msg.getButtonText()).isEqualTo("View");
            assertThat(msg.getSections()).hasSize(2);
            assertThat(msg.getSections().get(0).title()).isEqualTo("Makeup");
            assertThat(msg.getSections().get(0).rows()).hasSize(2);
            assertThat(msg.getSections().get(0).rows().get(0).id()).isEqualTo("bridal");
            assertThat(msg.getSections().get(0).rows().get(0).title()).isEqualTo("Bridal Makeup");
            assertThat(msg.getSections().get(0).rows().get(0).description()).isEqualTo("GHS 500");
            assertThat(msg.getSections().get(1).rows()).hasSize(1);
        }

        @Test
        @DisplayName("builds a list row without description")
        void shouldBuildRowWithoutDescription() {
            ListMessage msg = MessageBuilder.to(PHONE)
                    .list()
                    .body("Pick")
                    .section("Items", s -> s.row("id1", "Title Only"))
                    .build();

            assertThat(msg.getSections().get(0).rows().get(0).description()).isNull();
        }

        @Test
        @DisplayName("defaults buttonText to 'Select'")
        void shouldDefaultButtonText() {
            ListMessage msg = MessageBuilder.to(PHONE)
                    .list()
                    .body("Pick")
                    .section("Items", s -> s.row("id1", "Item"))
                    .build();

            assertThat(msg.getButtonText()).isEqualTo("Select");
        }

        @Test
        @DisplayName("throws when no sections added")
        void shouldThrowWhenNoSections() {
            assertThatThrownBy(() ->
                    MessageBuilder.to(PHONE).list().body("Empty").build()
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least one section");
        }
    }

    @Nested
    @DisplayName("ButtonMessage")
    class ButtonTests {

        @Test
        @DisplayName("builds a button message with 1-3 buttons")
        void shouldBuildButtonMessage() {
            ButtonMessage msg = MessageBuilder.to(PHONE)
                    .buttons()
                    .header("Confirm")
                    .body("Proceed?")
                    .footer("Reply below")
                    .button("yes", "Yes")
                    .button("no", "No")
                    .build();

            assertThat(msg.getTo()).isEqualTo(PHONE);
            assertThat(msg.getHeader()).isEqualTo("Confirm");
            assertThat(msg.getBody()).isEqualTo("Proceed?");
            assertThat(msg.getFooter()).isEqualTo("Reply below");
            assertThat(msg.getButtons()).hasSize(2);
            assertThat(msg.getButtons().get(0).id()).isEqualTo("yes");
            assertThat(msg.getButtons().get(0).title()).isEqualTo("Yes");
        }

        @Test
        @DisplayName("allows exactly 3 buttons")
        void shouldAllowThreeButtons() {
            ButtonMessage msg = MessageBuilder.to(PHONE)
                    .buttons()
                    .body("Choose")
                    .button("a", "A")
                    .button("b", "B")
                    .button("c", "C")
                    .build();

            assertThat(msg.getButtons()).hasSize(3);
        }

        @Test
        @DisplayName("throws when adding more than 3 buttons")
        void shouldThrowWhenMoreThanThreeButtons() {
            assertThatThrownBy(() ->
                    MessageBuilder.to(PHONE)
                            .buttons()
                            .body("Too many")
                            .button("a", "A")
                            .button("b", "B")
                            .button("c", "C")
                            .button("d", "D")
                            .build()
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("maximum of 3");
        }

        @Test
        @DisplayName("throws when no buttons added")
        void shouldThrowWhenNoButtons() {
            assertThatThrownBy(() ->
                    MessageBuilder.to(PHONE).buttons().body("Empty").build()
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least one button");
        }
    }
}
