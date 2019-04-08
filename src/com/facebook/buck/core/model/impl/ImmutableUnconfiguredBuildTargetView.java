/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.core.model.impl;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.Flavor;
import com.facebook.buck.core.model.ImmutableUnconfiguredBuildTargetData;
import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.core.model.UnconfiguredBuildTargetData;
import com.facebook.buck.core.model.UnconfiguredBuildTargetView;
import com.facebook.buck.core.model.UnflavoredBuildTargetView;
import com.facebook.buck.log.views.JsonViews;
import com.facebook.buck.util.RichStream;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSortedSet;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** An immutable implementation of {@link UnconfiguredBuildTargetView}. */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE)
public class ImmutableUnconfiguredBuildTargetView implements UnconfiguredBuildTargetView {

  private final UnconfiguredBuildTargetData data;
  private final UnflavoredBuildTargetView unflavoredBuildTargetView;
  private final int hash;

  private ImmutableUnconfiguredBuildTargetView(
      UnflavoredBuildTargetView unflavoredBuildTargetView, ImmutableSortedSet<Flavor> flavors) {
    this.data =
        ImmutableUnconfiguredBuildTargetData.of(unflavoredBuildTargetView.getData(), flavors);
    this.unflavoredBuildTargetView = unflavoredBuildTargetView;
    this.hash = Objects.hash(this.data, this.unflavoredBuildTargetView);
  }

  private ImmutableUnconfiguredBuildTargetView(Path cellPath, UnconfiguredBuildTargetData data) {
    this.data = data;
    this.unflavoredBuildTargetView =
        ImmutableUnflavoredBuildTargetView.of(cellPath, data.getUnflavoredBuildTarget());
    this.hash = Objects.hash(this.data, this.unflavoredBuildTargetView);
  }

  /**
   * Create new immutable instance of {@link UnconfiguredBuildTargetView}
   *
   * @param cellPath Absolute path to a cell containing this target
   * @param data Data object that backs this view
   */
  public static ImmutableUnconfiguredBuildTargetView of(
      Path cellPath, UnconfiguredBuildTargetData data) {
    return new ImmutableUnconfiguredBuildTargetView(cellPath, data);
  }

  /**
   * Create new immutable instance of {@link UnconfiguredBuildTargetView}
   *
   * @param unflavoredBuildTargetView Build target without flavors
   * @param flavors Flavors that apply to this build target
   */
  public static ImmutableUnconfiguredBuildTargetView of(
      UnflavoredBuildTargetView unflavoredBuildTargetView, ImmutableSortedSet<Flavor> flavors) {
    return new ImmutableUnconfiguredBuildTargetView(unflavoredBuildTargetView, flavors);
  }

  /**
   * Create new immutable instance of {@link UnconfiguredBuildTargetView}
   *
   * @param unflavoredBuildTargetView Build target without flavors
   * @param flavors Flavors that apply to this build target
   */
  public static ImmutableUnconfiguredBuildTargetView of(
      UnflavoredBuildTargetView unflavoredBuildTargetView, RichStream<Flavor> flavors) {
    return of(
        unflavoredBuildTargetView,
        flavors.toImmutableSortedSet(UnconfiguredBuildTargetData.FLAVOR_ORDERING));
  }

  /**
   * Create new immutable instance of {@link UnconfiguredBuildTargetView}
   *
   * @param unflavoredBuildTargetView Build target without flavors
   * @param flavors Flavors that apply to this build target
   */
  public static ImmutableUnconfiguredBuildTargetView of(
      UnflavoredBuildTargetView unflavoredBuildTargetView, Stream<Flavor> flavors) {
    return of(unflavoredBuildTargetView, RichStream.from(flavors));
  }

  /**
   * Create new immutable instance of {@link UnconfiguredBuildTargetView} that has no flavors
   *
   * @param unflavoredBuildTargetView Build target without flavors
   */
  public static ImmutableUnconfiguredBuildTargetView of(
      UnflavoredBuildTargetView unflavoredBuildTargetView) {
    return of(unflavoredBuildTargetView, ImmutableSortedSet.of());
  }

  /** Helper for creating a build target with no flavors and no cell name. */
  public static ImmutableUnconfiguredBuildTargetView of(
      Path cellPath, String baseName, String shortName) {
    return ImmutableUnconfiguredBuildTargetView.of(
        ImmutableUnflavoredBuildTargetView.of(cellPath, Optional.empty(), baseName, shortName));
  }

  @JsonIgnore
  @Override
  public UnflavoredBuildTargetView getUnflavoredBuildTargetView() {
    return unflavoredBuildTargetView;
  }

  @JsonIgnore
  @Override
  public ImmutableSortedSet<Flavor> getFlavors() {
    return data.getFlavors();
  }

  @JsonProperty("cell")
  @Override
  public Optional<String> getCell() {
    String cell = data.getUnflavoredBuildTarget().getCell();
    return cell == "" ? Optional.empty() : Optional.of(cell);
  }

  @JsonIgnore
  @Override
  public Path getCellPath() {
    return unflavoredBuildTargetView.getCellPath();
  }

  @JsonProperty("baseName")
  @JsonView(JsonViews.MachineReadableLog.class)
  @Override
  public String getBaseName() {
    return data.getUnflavoredBuildTarget().getBaseName();
  }

  @JsonIgnore
  @Override
  public Path getBasePath() {
    return unflavoredBuildTargetView.getBasePath();
  }

  @JsonProperty("shortName")
  @JsonView(JsonViews.MachineReadableLog.class)
  @Override
  public String getShortName() {
    return data.getUnflavoredBuildTarget().getName();
  }

  @JsonIgnore
  @Override
  public String getShortNameAndFlavorPostfix() {
    return getShortName() + getFlavorPostfix();
  }

  @JsonProperty("flavor")
  @JsonView(JsonViews.MachineReadableLog.class)
  private String getFlavorsAsString() {
    return Joiner.on(",").join(getFlavors());
  }

  private String getFlavorPostfix() {
    if (getFlavors().isEmpty()) {
      return "";
    }
    return "#" + getFlavorsAsString();
  }

  private @Nullable String fullyQualifiedName = null;

  @JsonIgnore
  @Override
  public String getFullyQualifiedName() {
    if (fullyQualifiedName == null) {
      fullyQualifiedName = unflavoredBuildTargetView.getFullyQualifiedName() + getFlavorPostfix();
    }
    return fullyQualifiedName;
  }

  @JsonIgnore
  @Override
  public boolean isFlavored() {
    return !getFlavors().isEmpty();
  }

  @Override
  public UnconfiguredBuildTargetView assertUnflavored() {
    Preconditions.checkState(!isFlavored(), "%s is flavored.", this);
    return this;
  }

  @Override
  public UnconfiguredBuildTargetView withShortName(String shortName) {
    return ImmutableUnconfiguredBuildTargetView.of(
        ImmutableUnflavoredBuildTargetView.of(
            getUnflavoredBuildTargetView().getCellPath(),
            getUnflavoredBuildTargetView().getCell(),
            getUnflavoredBuildTargetView().getBaseName(),
            shortName),
        getFlavors());
  }

  @Override
  public UnconfiguredBuildTargetView withFlavors(Flavor... flavors) {
    return withFlavors(Arrays.asList(flavors));
  }

  @Override
  @SuppressWarnings("unchecked")
  public UnconfiguredBuildTargetView withFlavors(Iterable<? extends Flavor> flavors) {
    ImmutableSortedSet<Flavor> flavorsSet;
    if (flavors instanceof ImmutableSortedSet
        && ((ImmutableSortedSet<Flavor>) flavors)
            .comparator()
            .equals(UnconfiguredBuildTargetData.FLAVOR_ORDERING)) {
      flavorsSet = (ImmutableSortedSet<Flavor>) flavors;
    } else {
      flavorsSet = ImmutableSortedSet.copyOf(UnconfiguredBuildTargetData.FLAVOR_ORDERING, flavors);
    }

    return ImmutableUnconfiguredBuildTargetView.of(unflavoredBuildTargetView, flavorsSet);
  }

  @Override
  public UnconfiguredBuildTargetView withUnflavoredBuildTarget(UnflavoredBuildTargetView target) {
    return ImmutableUnconfiguredBuildTargetView.of(target, getFlavors());
  }

  @Override
  public UnconfiguredBuildTargetView withoutCell() {
    return ImmutableUnconfiguredBuildTargetView.of(
        ImmutableUnflavoredBuildTargetView.of(
            getCellPath(), Optional.empty(), getBaseName(), getShortName()),
        getFlavors());
  }

  @Override
  public BuildTarget configure(TargetConfiguration targetConfiguration) {
    return ImmutableBuildTarget.of(this, targetConfiguration);
  }

  @Override
  public UnconfiguredBuildTargetData getData() {
    return data;
  }

  /** @return {@link #getFullyQualifiedName()} */
  @Override
  public String toString() {
    return getFullyQualifiedName();
  }

  @Override
  public boolean equals(Object another) {
    if (this == another) {
      return true;
    }
    return another instanceof ImmutableUnconfiguredBuildTargetView
        && equalTo((ImmutableUnconfiguredBuildTargetView) another);
  }

  private boolean equalTo(ImmutableUnconfiguredBuildTargetView another) {
    // Do not use 'unflavoredBuildTargetView' - everything except cell path is in 'data'
    // And we do not want to compare with absolute paths
    return Objects.equals(data, another.data)
        && Objects.equals(unflavoredBuildTargetView, another.unflavoredBuildTargetView);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public int compareTo(UnconfiguredBuildTargetView o) {
    if (this == o) {
      return 0;
    }

    if (!(o instanceof ImmutableUnconfiguredBuildTargetView)) {
      return ImmutableUnconfiguredBuildTargetView.class.getName().compareTo(o.getClass().getName());
    }

    ImmutableUnconfiguredBuildTargetView other = (ImmutableUnconfiguredBuildTargetView) o;

    return ComparisonChain.start()
        .compare(data, other.data)
        .compare(unflavoredBuildTargetView, other.unflavoredBuildTargetView)
        .result();
  }
}