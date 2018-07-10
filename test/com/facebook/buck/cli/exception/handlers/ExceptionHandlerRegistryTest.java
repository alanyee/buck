/*
 * Copyright 2018-present Facebook, Inc.
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

package com.facebook.buck.cli.exception.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import com.facebook.buck.cli.exceptions.handlers.ExceptionHandlerRegistryFactory;
import com.facebook.buck.core.exceptions.HumanReadableException;
import com.facebook.buck.core.exceptions.handler.ExceptionHandlerRegistry;
import com.facebook.buck.core.exceptions.handler.HumanReadableExceptionAugmentor;
import com.facebook.buck.parser.exceptions.BuildFileParseException;
import com.facebook.buck.testutil.TestConsole;
import com.facebook.buck.util.BuckIsDyingException;
import com.facebook.buck.util.CommandLineException;
import com.facebook.buck.util.ExitCode;
import com.facebook.buck.util.InterruptionFailedException;
import com.google.common.collect.ImmutableMap;
import com.martiansoftware.nailgun.NGContext;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.FileSystemLoopException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;

public class ExceptionHandlerRegistryTest {

  private ExceptionHandlerRegistry registry;
  private TestConsole console;
  private Optional<NGContext> ngContext = Optional.empty();

  @Before
  public void setUp() {
    console = new TestConsole();
    registry =
        ExceptionHandlerRegistryFactory.create(
            console, ngContext, new HumanReadableExceptionAugmentor(ImmutableMap.of()));
  }

  @Test
  public void testWithUnhandledExecutionException() {
    ExecutionException ex = new ExecutionException(new Throwable());
    assertThat(registry.handleException(ex), is(ExitCode.FATAL_GENERIC));
    assertThat(getTextWrittenToStdErr(), containsString("java.lang.Throwable"));
  }

  @Test
  public void testWithWrappedHumanReadableException() {
    ExecutionException ex =
        new ExecutionException(
            "coming from Future, should be ignored",
            new HumanReadableException("useful exception"));
    assertThat(registry.handleException(ex), is(ExitCode.BUILD_ERROR));
    assertThat(getTextWrittenToStdErr(), containsString("useful exception"));
  }

  @Test
  public void testWithDoubleWrappedInterruptedException() {
    ExecutionException ex =
        new ExecutionException(
            "coming from Future, should be ignored",
            new ExecutionException(
                "coming from Future, should be ignored",
                new InterruptedException("user interrupted exception")));
    assertThat(registry.handleException(ex), is(ExitCode.SIGNAL_INTERRUPT));
    assertThat(getTextWrittenToStdErr(), is(emptyString()));
  }

  @Test
  public void testWithClosedByInterruptException() {
    assertThat(
        registry.handleException(new ClosedByInterruptException()), is(ExitCode.SIGNAL_INTERRUPT));
    assertThat(getTextWrittenToStdErr(), is(emptyString()));
  }

  @Test
  public void testWithDiskFullException() {
    String noDiskSpaceMessage = "No space left on device";
    assertThat(
        registry.handleException(new IOException(noDiskSpaceMessage)),
        is(ExitCode.FATAL_DISK_FULL));
    assertThat(getTextWrittenToStdErr(), containsString(noDiskSpaceMessage));
  }

  @Test
  public void testWithFatalIOException() {
    String fatalIOExceptionMessage = "Fatal IO Exception, not disk full, not FileSystemLoop";
    assertThat(
        registry.handleException(new IOException(fatalIOExceptionMessage)), is(ExitCode.FATAL_IO));
    assertThat(getTextWrittenToStdErr(), containsString(fatalIOExceptionMessage));
  }

  @Test
  public void testWithCommandLineException() {
    assertThat(
        registry.handleException(new CommandLineException("command line exception")),
        is(ExitCode.COMMANDLINE_ERROR));
    assertThat(getTextWrittenToStdErr(), containsString("BAD ARGUMENTS:"));
  }

  @Test
  public void testWithFileLoopException() {
    assertThat(
        registry.handleException(new FileSystemLoopException("Symlink found")),
        is(ExitCode.FATAL_GENERIC));
    String consoleText = getTextWrittenToStdErr();
    assertThat(consoleText, containsString("Loop detected"));
    assertThat(consoleText, containsString("Symlink found"));
  }

  @Test
  public void testWithWrappedOOMError() {
    assertThat(
        registry.handleException(
            new ExecutionException("coming from Future, will be ignored", new OutOfMemoryError())),
        is(ExitCode.FATAL_OOM));
    assertThat(getTextWrittenToStdErr(), containsString("Buck ran out of memory"));
  }

  @Test
  public void testWithInterruptionFailedException() {
    assertThat(
        registry.handleException(new InterruptionFailedException("Interruption failed")),
        is(ExitCode.SIGNAL_INTERRUPT));
    assertThat(getTextWrittenToStdErr(), is(emptyString()));
  }

  @Test
  public void testWithBuildFileParseException() {
    String parserErrorMessage = "Unknown parser error";
    assertThat(
        registry.handleException(
            BuildFileParseException.createForUnknownParseError(parserErrorMessage)),
        is(ExitCode.PARSE_ERROR));
    assertThat(getTextWrittenToStdErr(), containsString(parserErrorMessage));
  }

  @Test
  public void testWithBuckIsDyingException() {
    assertThat(
        registry.handleException(new BuckIsDyingException("Buck is dying")),
        is(ExitCode.FATAL_GENERIC));
    String consoleText = getTextWrittenToStdErr();
    assertThat(consoleText, containsString("Fallout because buck was already dying"));
  }

  @Test
  public void testWithThrowable() {
    String throwableMessage = "java.lang.Throwable: this is a throwable";
    assertThat(
        registry.handleException(new Throwable(throwableMessage)), is(ExitCode.FATAL_GENERIC));
    String consoleText = getTextWrittenToStdErr();
    assertThat(consoleText, containsString(throwableMessage));
  }

  @Test
  public void testWithThrowableWithLoopInCauses() {
    String throwableMessage = "this is a throwable with a loop in its causes";
    Exception t4 = new Exception("t4");
    Exception t0 =
        new Exception(
            throwableMessage, new Exception("t1", new Exception("t2", new Exception("t3", t4))));
    t4.initCause(t0.getCause());
    assertThat(registry.handleException(t0), is(ExitCode.FATAL_GENERIC));
    String consoleText = getTextWrittenToStdErr();
    assertThat(consoleText, containsString(throwableMessage));
  }

  public String getTextWrittenToStdErr() {
    String text = console.getTextWrittenToStdErr();
    System.err.println("-- Captured test console stderr --");
    System.err.println(text);
    System.err.println("----");
    return text;
  }
}
