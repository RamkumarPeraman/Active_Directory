// import Controller from '@ember/controller';
// import { action } from '@ember/object';
// import { tracked } from '@glimmer/tracking';

// export default class UsersController extends Controller {
//   @tracked users = [];
//   @tracked selectedUser = null;

//   @action
//   async fetchUsers(params = {}) {
//     const query = new URLSearchParams(params).toString();
//     const url = `http://localhost:8080/backend_war_exploded/UserServlet?${query}`;

//     try {
//       const response = await fetch(url);
//       if (!response.ok) {
//         throw new Error(`Failed to fetch users: ${response.statusText}`);
//       }
//       this.users = await response.json();
//     } catch (error) {
//       console.error('Error fetching users:', error);
//       this.users = [];
//     }
//   }

//   @action
//   async showUserDetails(userId) {
//     try {
//       const response = await fetch(
//         `http://localhost:8080/backend_war_exploded/UserServlet?id=${userId}`,
//       );
//       if (!response.ok) {
//         throw new Error(`Failed to fetch user details: ${response.statusText}`);
//       }
//       this.selectedUser = await response.json();
//     } catch (error) {
//       console.error('Error fetching user details:', error);
//       this.selectedUser = null;
//     }
//   }

//   @action
//   closePopup() {
//     this.selectedUser = null;
//   }
// }



import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class UserController extends Controller {
  @tracked users = [];
  @tracked selectedUser = null;
  @tracked searchQuery = ''; // To store the search input
  @tracked sortBy = ''; // To store the sort option

  // Fetch users with search and sort parameters
  @action
  async fetchUsers() {
    const params = {
      search: this.searchQuery, // Send searchQuery as a parameter to the backend
      sortBy: this.sortBy, // Send sort parameter to the backend
    };
    const query = new URLSearchParams(params).toString();
    const url = `http://localhost:8080/backend_war_exploded/UserServlet?${query}`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to fetch users: ${response.statusText}`);
      }
      this.users = await response.json();
    } catch (error) {
      console.error('Error fetching users:', error);
      this.users = [];
    }
  }

  // Fetch details of a selected user
  @action
  async showUserDetails(userId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/UserServlet?id=${userId}`,
      );
      if (!response.ok) {
        throw new Error(`Failed to fetch user details: ${response.statusText}`);
      }
      this.selectedUser = await response.json();
    } catch (error) {
      console.error('Error fetching user details:', error);
      this.selectedUser = null;
    }
  }

  // Close the user details popup
  @action
  closePopup() {
    this.selectedUser = null;
  }

  // Update the search query and fetch users
  @action
  updateSearchQuery(event) {
    this.searchQuery = event.target.value;
    this.fetchUsers(); // Trigger fetch with new search query
  }

  // Update the sort option and fetch users accordingly
  @action
  updateSortBy(event) {
    this.sortBy = event.target.value;
    this.fetchUsers(); // Trigger fetch with new sort option
  }

  // Lifecycle hook to load users when the controller is initialized
  constructor() {
    super(...arguments);
    this.fetchUsers(); // Fetch users when the controller is initialized
  }
}
