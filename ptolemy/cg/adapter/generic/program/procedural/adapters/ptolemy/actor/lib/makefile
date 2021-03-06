# Makefile for Ptolemy Classic style Codegen
#
# @Version: $Id$
# @Author: Christopher Brooks
#
# @Copyright (c) 2005-2016 The Regents of the University of California
# All rights reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
#
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA  OR RESEARCH IN MOTION
# LIMITED BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
# OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND
# ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA OR
# RESEARCH IN MOTION LIMITED HAVE BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA  AND RESEARCH IN MOTION LIMITED
# HAVE NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
#
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY
##########################################################################

ME =		ptolemy/cg/adapter/generic/program/procedural/adapters/ptolemy/actor/lib

# PTJJS_DIR is set to jjs if we Nashorn, which is a part of Java 1.8, is present.
DIRS =		conversions fmi gui hoc $(PTJJS_DIR) logic string

# Root of the Java directory
ROOT =		../../../../../../../../../..

CLASSPATH =	$(ROOT)

# Get configuration info
CONFIG =	$(ROOT)/mk/ptII.mk
include $(CONFIG)

# Used to build jar files
PTPACKAGE = 	lib
PTCLASSJAR =

PTCLASSALLJAR = $(PTPACKAGE).jar

# Include the .class files from these jars in PTCLASSALLJAR
# If you change this, change DIRS above
PTCLASSALLJARS = \
	conversions/conversions.jar \
	fmi/fmi.jar \
	gui/gui.jar \
	hoc/hoc.jar \
	logic/logic.jar \
	string/string.jar

# Keep this list alphabetized.
JSRCS = \
	AbsoluteValue.java \
	Accumulator.java \
	AddSubtract.java \
	Bernoulli.java \
	BooleanMultiplexor.java \
	BooleanSwitch.java \
	Commutator.java \
	Const.java \
	CurrentTime.java \
	DB.java \
	Differential.java \
	Discard.java \
	Distributor.java \
	Gaussian.java \
	LimitedFiringSource.java \
	Limiter.java \
	Minimum.java \
	MovingAverage.java \
	MultiplyDivide.java \
	Publisher.java \
	Pulse.java \
	Ramp.java \
	RandomSource.java \
	Recorder.java \
	Scale.java \
	Sequence.java \
	StringConst.java \
	Subscriber.java \
	SubscriptionAggregator.java \
	Test.java \
	TrigFunction.java \
	VectorAssembler.java \
	VectorDisassembler.java

OTHER_FILES_TO_BE_JARED = 

EXTRA_SRCS =	$(JSRCS) $(OTHER_FILES_TO_BE_JARED)

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
# Don't include demo or DIRS here, or else 'make sources' will run 'make demo'
MISC_FILES =	gui conversions fmi hoc logic string

# make checkjunk will not report OPTIONAL_FILES as trash
# make distclean removes OPTIONAL_FILES
OPTIONAL_FILES = \
	demo \
	jjs

JCLASS = $(JSRCS:%.java=%.class)

all: jclass

install: all jars $(PTPACKAGE).jar $(PTPACKAGE_NATIVE_JAR)

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
