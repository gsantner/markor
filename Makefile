# License of Makefile: Public Domain / CC0
.PHONY: $(shell sed -n -e '/^$$/ { n ; /^[^ .\#][^ ]*:/ { s/:.*$$// ; p ; } ; }' $(MAKEFILE_LIST))
.NOTPARALLEL: clean
.DEFAULT_GOAL := all

env-%:
	@: $(if ${${*}},,$(error Environment variable $* not set))
####################################################################################

DIST_DIR = dist
MOVE = mv

all: $(DIST_DIR) build test lint

####################################################################################

$(DIST_DIR):
	mkdir -p ${DIST_DIR}

.NOTPARALLEL: gradle
gradle: env-ANDROID_SDK_ROOT
	mkdir -p $(DIST_DIR)/log/
	chmod +x gradlew
	./gradlew --no-daemon --parallel --stacktrace $A  2>&1 | tee "$(DIST_DIR)/log/gradle$L.log"
	@echo "-----------------------------------------------------------------------------------"
	cat "$(DIST_DIR)/log/gradle$L.log" | grep "BUILD" | tail -n1 | grep -q "BUILD SUCCESSFUL in"

build:
	rm -f $(DIST_DIR)/*.apk
	$(MAKE) L="-build" A="clean assembleFlavorAtest -x lint" gradle
	find app -type f -iname '*.apk' | grep -v 'unsigned.apk' | xargs cp -R -t $(DIST_DIR)/

lint:
	rm -Rf $(DIST_DIR)/*lint*
	mkdir -p $(DIST_DIR)/lint/
	$(MAKE) L="-lint" A="lintFlavorDefaultDebug" gradle
	find app -type f -iname 'lint-results-*' | xargs cp -R -t $(DIST_DIR)/lint

test:
	rm -Rf $(DIST_DIR)/unittest $(DIST_DIR)/*UnitTest*
	$(MAKE) L="-unittest" A="testFlavorDefaultDebugUnitTest -x lint" gradle
	find app -type d -iname 'testFlavorDefaultDebugUnitTest' | xargs cp -R -t $(DIST_DIR)/
	mv ${DIST_DIR}/testFlavorDefaultDebugUnitTest $(DIST_DIR)/tests

clean:
	$(MAKE) A="clean" gradle
	rm -Rf $(DIST_DIR)
	$(MAKE) $(DIST_DIR)
