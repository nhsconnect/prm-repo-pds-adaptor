package uk.nhs.prm.deductions.pdsadaptor.testing;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MapBuilder {
    public static Map<String, Object> create(Consumer<MapBuilder> spec) {
        MapBuilder builder = new MapBuilder();
        spec.accept(builder);
        return builder.build();
    }

    public static String json(Consumer<MapBuilder> spec) {
        MapBuilder builder = new MapBuilder();
        spec.accept(builder);
        return builder.toJson();
    }

    private Map<String, Object> map = new HashMap<>();

    public static MapBuilder mapBuilder() {
        return new MapBuilder();
    }

    public MapBuilder with(String key, Object value) {
        map.put(key, value);
        return this;
    }

    public MapBuilder with(String key, Consumer<MapBuilder> nestedMapSpec) {
        return nesting(key, nestedMapSpec);
    }

    public MapBuilder kv(String key, Object value) {
        return with(key, value);
    }

    public MapBuilder kv(String key, Consumer<MapBuilder> nestedMapSpec) {
        return with(key, nestedMapSpec);
    }

    public MapBuilder nesting(String key, Consumer<MapBuilder> nestedMapSpec) {
        map.put(key, create(nestedMapSpec));
        return this;
    }

    public Map<String, Object> build() {
        return map;
    }

    public String toJson() {
        return new JSONObject(map).toString();
    }
}
