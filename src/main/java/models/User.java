package models;

public class User {
        private String userName;
        private UserType userType;

        public User(String userName, UserType userType) {
            this.userName = userName;
            this.userType = userType;
        }

        public User(String userName, String type) {
            this.userName = userName;

            UserType userType = UserType.EMPLOYEE;
            if (type.equals("-G")) {
                userType = UserType.GUEST;
            }

            this.userType = userType;
        }

        public enum UserType {
            EMPLOYEE,
            GUEST
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return userName.equals(user.userName) && userType == user.userType;
        }

        @Override
        public int hashCode() {
            int result = userName.hashCode();
            result = 31 * result + userType.hashCode();
            return result;
        }

        public String getUserName() {
            return userName;
        }

        public UserType getUserType() {
            return userType;
        }
    }
