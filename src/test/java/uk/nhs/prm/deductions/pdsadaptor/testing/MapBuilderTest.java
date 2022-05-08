package uk.nhs.prm.deductions.pdsadaptor.testing;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.prm.deductions.pdsadaptor.testing.MapBuilder.*;

public class MapBuilderTest {

    @Test
    public void mapBuilderShouldMakeAFlatMapCheaply() {
        var map = mapBuilder().with("bob", "foo").with("sue", "too").build();

        assertThat(map.get("bob")).isEqualTo("foo");
        assertThat(map.get("sue")).isEqualTo("too");
    }

    @Test
    public void mapBuilderShouldProvideCreateMethodForSingleCallCreationWithLambda() {
        var map = create(mb -> mb
                .with("bob", "foo")
                .with("sue", "too"));

        assertThat(map.get("bob")).isEqualTo("foo");
        assertThat(map.get("sue")).isEqualTo("too");
    }

    @Test
    public void createShouldAllowNesting() {
        var map = create(m -> m
                .with("foo", create(a -> a.with("a", "ha")))
                .with("noo", "now"));

        assertThat(map.get("foo")).isInstanceOf(Map.class);
        assertThat(((Map) map.get("foo")).get("a")).isEqualTo("ha");
        assertThat(map.get("noo")).isEqualTo("now");
    }

    @Test
    public void createShouldProvideAnExplicitNestingForm() {
        var map = create(m -> m
                .nesting("foo", a -> a.with("a", "ha"))
                .with("noo", "now"));

        assertThat(map.get("foo")).isInstanceOf(Map.class);
        assertThat(((Map) map.get("foo")).get("a")).isEqualTo("ha");
        assertThat(map.get("noo")).isEqualTo("now");
    }

    @Test
    public void createShouldProvideAnImplicitNestingForm() {
        var map = create(m -> m
                .with("foo", a -> a.with("a", "ha"))
                .with("noo", "now"));

        assertThat(map.get("foo")).isInstanceOf(Map.class);
        assertThat(((Map) map.get("foo")).get("a")).isEqualTo("ha");
        assertThat(map.get("noo")).isEqualTo("now");
    }

    @Test
    public void shouldProvideShortReasonableAliases() {
        var map = create(m -> m
                .kv("foo", a -> a.kv("a", "ha"))
                .kv("noo", "now"));

        assertThat(map.get("foo")).isInstanceOf(Map.class);
        assertThat(((Map) map.get("foo")).get("a")).isEqualTo("ha");
        assertThat(map.get("noo")).isEqualTo("now");
    }

    @Test
    public void canAlsoProvideJson() {
        var json = mapBuilder()
                .kv("foo", a -> a.kv("a", "ha"))
                .kv("noo", "now").toJson();

        var map = new JSONObject(json);
        assertThat(((JSONObject) map.get("foo")).get("a")).isEqualTo("ha");
        assertThat(map.get("noo")).isEqualTo("now");
    }

    @Test
    public void andHasASweetShortFormForJsonCreation() {
        var json = json(jb -> jb
                .kv("foo", a -> a.kv("a", "ha"))
                .kv("noo", "now"));

        var map = new JSONObject(json);
        assertThat(((JSONObject) map.get("foo")).get("a")).isEqualTo("ha");
        assertThat(map.get("noo")).isEqualTo("now");
    }

}

