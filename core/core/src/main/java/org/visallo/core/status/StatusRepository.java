package org.visallo.core.status;

public interface StatusRepository {
    StatusHandle saveStatus(String group, String instance, StatusData statusData);

    void deleteStatus(StatusHandle statusHandle);

    Iterable<String> getGroups();

    Iterable<String> getInstances(String group);

    StatusData getStatusData(String group, String instance);

    class StatusHandle {
        private final String group;
        private final String instance;

        protected StatusHandle(String group, String instance) {
            this.group = group;
            this.instance = instance;
        }

        public String getGroup() {
            return group;
        }

        public String getInstance() {
            return instance;
        }

        @Override
        public String toString() {
            return "StatusHandle{" +
                    "group='" + group + '\'' +
                    ", instance='" + instance + '\'' +
                    '}';
        }
    }
}
