package test;

import io.vavr.control.Try;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

  public static final String EMPTY_STRING = "";

  public static <T> Predicate<T> not(Predicate<T> predicate) {
    return predicate.negate();
  }

  public static <T> Stream<T> checkStream(Collection<T> collection) {

    return Optional.ofNullable(collection).map(Collection::stream).orElseGet(Stream::empty);
  }

  public static <T> Iterator<T> checkIterator(Collection<T> collection) {

    return Optional.ofNullable(collection)
        .map(Collection::iterator)
        .orElseGet(Collections::emptyIterator);
  }

  public static LocalDate parseDateImport(String date) {

    return Try.of(() -> Optional.ofNullable(date))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(LocalDate::parse)
        .getOrNull();
  }

  public static <T> boolean isEmptyList(List<T> list) {
    return Optional.ofNullable(list).map(List::isEmpty).orElseGet(() -> Boolean.TRUE);
  }

  public static <T> List<T> addToList(List<T> list, T element) {

    return addToList(list, Arrays.asList(element));
  }

  public static <T> List<T> addToList(List<T> list, List<T> elements) {

    if (Utils.isEmptyList(elements)) {
      return list;
    }
    if (Utils.isEmptyList(list)) {
      return elements;
    }
    return Stream.of(list, elements)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
  }

  public static <T> List<T> removeFromList(List<T> list, T element) {

    if (Objects.isNull(element)) {
      return list;
    }
    return Utils.checkStream(list)
        .filter(Objects::nonNull)
        .filter(e -> !e.equals(element))
        .distinct()
        .collect(Collectors.toList());
  }

  public static <T> UnaryOperator<T> peek(Consumer<T> c) {
    return x -> {
      c.accept(x);
      return x;
    };
  }

  public static void sleepMiliSeconds(int miliSeconds) {

    Try.run(() -> Thread.sleep(miliSeconds * 1l))
        .onFailure(e -> Thread.currentThread().interrupt());
  }

  public static void sleepSeconds(int seconds) {

    sleepMiliSeconds(seconds * 1000);
  }

  public static void sleepMinutes(int minutes) {
    sleepSeconds(minutes * 60);
  }

  public static void sleepHours(int hours) {
    sleepMinutes(hours * 60);
  }

  public static <T> boolean equalLists(List<T> list1, List<T> list2) {

    if (Objects.isNull(list1) || Objects.isNull(list2)) {
      return false;
    }
    if (list1.size() != list2.size()) {
      return false;
    }

    for (int i = 0; i < list1.size(); i++) {
      if (!list1.get(i).equals(list2.get(i))) {
        return false;
      }
    }
    return true;
  }
}
