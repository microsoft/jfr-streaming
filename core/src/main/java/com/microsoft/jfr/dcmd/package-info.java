// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
/**
 * This package provides API for controlling Java Flight Recordings (JFR)
 * through the DiagnosticCommand MBean. The code in this package is meant to
 * provide a fallback for starting, stopping, and dumping a Java Flight Recording if
 * {@link com.microsoft.jfr.FlightRecorderConnection#connect(javax.management.MBeanServerConnection)}
 * throws an {@code InstanceNotFoundException} on a Java 8 JVM.
 */
package com.microsoft.jfr.dcmd;
