package de.mrjulsen.wires.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class DLStatistics {

    public static final DLStatistics EMPTY = new DLStatistics("", List.of());

    public static record Group(String id, String name) {}
    public static record Stat(Group group, String name, Object value) {}

    private final String name;
    private final Map<Group, List<Stat>> stats;

    // Konstruktor
    public DLStatistics(String name, List<Stat> stats) {
        this.stats = new LinkedHashMap<>();
        for (Stat stat : stats) {
            this.stats.computeIfAbsent(stat.group(), g -> new ArrayList<>()).add(stat);
        }
        this.name = name;
    }

    private DLStatistics(String name, Map<Group, List<Stat>> stats) {
        this.name = name;
        this.stats = stats;
    }

    public String getName() {
        return name;
    }

    public String print(boolean withStatNames) {
        
        return name + ": " + stats.entrySet().stream()
                .map(entry -> {
                    String groupName = entry.getKey().name();
                    List<Stat> values = entry.getValue();

                    String formattedValues;
                    if (values.size() == 1) {
                        Stat x = values.get(0);
                        String str = String.valueOf(x.value());
                        if (withStatNames) {
                            str = x.name() + ": " + str;
                        }
                        formattedValues = str;
                    } else {
                        formattedValues = values.stream()
                                .map(x -> {
                                    String str = String.valueOf(x.value());
                                    if (withStatNames) {
                                        str = x.name() + ": " + str;
                                    }
                                    return str;
                                })
                                .collect(Collectors.joining(", ", "[", "]"));
                    }

                    return groupName + ": " + formattedValues;
                })
                .collect(Collectors.joining(", "));
    }

    @SafeVarargs
    public static DLStatistics merge(String name, BiFunction<Object, Object, Object> mergeFunction, DLStatistics... statistics) {
        Map<Group, Map<String, Stat>> merged = new LinkedHashMap<>();

        for (DLStatistics statistic : statistics) {
            for (List<Stat> stat : statistic.stats.values()) {
                for (Stat s : stat) {
                    merged.computeIfAbsent(s.group(), g -> new LinkedHashMap<>()).merge(s.name(), s, (a, b) -> new Stat(a.group(), a.name(), mergeFunction.apply(a.value(), b.value())));
                }
            }
        }

        Map<Group, List<Stat>> result = new LinkedHashMap<>();        
        for (Map.Entry<Group, Map<String, Stat>> stat : merged.entrySet()) {
            for (Stat s : stat.getValue().values()) {
                result.computeIfAbsent(s.group(), g -> new ArrayList<>()).add(s);
            }
        }

        return new DLStatistics(name, result);
    }
}
