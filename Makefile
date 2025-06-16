MILL := mill
TASK_TARGET := redef[]

.PHONY: bloop
bloop:
	${MILL} bloop.install

.PHONY: fmt
fmt:
	${MILL} format

fix-check:
	${MILL} ${TASK_TARGET}.fix --check

.PHONY: compile
compile:
	${MILL} ${TASK_TARGET}.compile

.PHONY: test
test:
	${MILL} ${TASK_TARGET}.test

.PHONY: clean
clean:
	${MILL} clean

cloc:
	cloc --vcs=git
