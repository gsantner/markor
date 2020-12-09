# License of Makefile: Public Domain / CC0
.PHONY: $(shell sed -n -e '/^$$/ { n ; /^[^ .\#][^ ]*:/ { s/:.*$$// ; p ; } ; }' $(MAKEFILE_LIST))
.NOTPARALLEL: clean
.DEFAULT_GOAL := all

env-%:
	@: $(if ${${*}},,$(error Environment variable $* not set))
####################################################################################

DIST_DIR = dist
MOVE = mv

all: $(DIST_DIR) lint test build

####################################################################################

$(DIST_DIR):
	mkdir -p ${DIST_DIR}

.NOTPARALLEL: gradle gradle-check-error
gradle: env-ANDROID_SDK_ROOT
	mkdir -p $(DIST_DIR)/log/
	chmod +x gradlew
	./gradlew --no-daemon --parallel --stacktrace $A  2>&1 | tee "$(DIST_DIR)/log/gradle.log"
	@echo "-----------------------------------------------------------------------------------"

gradle-check-error:
	mv  "$(DIST_DIR)/log/gradle.log" "$(DIST_DIR)/log/gradle$A.log"
	cat "$(DIST_DIR)/log/gradle$A.log" | grep "BUILD " | tail -n1 | grep -q "BUILD SUCCESSFUL in"

build:
	rm -f $(DIST_DIR)/*.apk
	$(MAKE) A="clean assembleFlavorAtest -x lint" gradle
	find app -type f -iname '*.apk' | grep -v 'unsigned.apk' | xargs cp -R -t $(DIST_DIR)/
	$(MAKE) A="-build" gradle-check-error

lint:
	rm -Rf $(DIST_DIR)/lint
	mkdir -p $(DIST_DIR)/lint/
	$(MAKE) A="lintFlavorDefaultDebug" gradle
	find app -type f -iname 'lint-results-*' | xargs cp -R -t $(DIST_DIR)/lint
	$(MAKE) A="-lint" gradle-check-error

test:
	rm -Rf $(DIST_DIR)/tests
	$(MAKE) A="testFlavorDefaultDebugUnitTest -x lint" gradle
	mkdir -p app/build/test-results/testFlavorDefaultDebugUnitTest && echo 'PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHRlc3RzdWl0ZSBuYW1lPSJkdW1teSIgdGVzdHM9IjEiIHNraXBwZWQ9IjAiIGZhaWx1cmVzPSIwIiBlcnJvcnM9IjAiIHRpbWVzdGFtcD0iMjAyMC0xMi0wOFQwMDowMDowMCIgaG9zdG5hbWU9ImxvY2FsaG9zdCIgdGltZT0iMC4wMSI+CiAgPHByb3BlcnRpZXMvPgogIDx0ZXN0Y2FzZSBuYW1lPSJkdW1teSIgY2xhc3NuYW1lPSJkdW1teSIgdGltZT0iMC4wMSIvPgogIDxzeXN0ZW0tb3V0PjwhW0NEQVRBW11dPjwvc3lzdGVtLW91dD4KICA8c3lzdGVtLWVycj48IVtDREFUQVtdXT48L3N5c3RlbS1lcnI+CjwvdGVzdHN1aXRlPgo=' | base64 -d > 'app/build/test-results/testFlavorDefaultDebugUnitTest/TEST-dummy.xml'
	find app -type d -iname 'testFlavorDefaultDebugUnitTest' | xargs cp -R -t $(DIST_DIR)/
	mv ${DIST_DIR}/testFlavorDefaultDebugUnitTest $(DIST_DIR)/tests
	$(MAKE) A="-test" gradle-check-error

clean:
	$(MAKE) A="clean" gradle
	rm -Rf $(DIST_DIR) app/build app/flavor*
	$(MAKE) $(DIST_DIR)
