import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class UsersController extends Controller {
  @tracked users = [];
  @tracked selectedUser = null;

  @action
  async fetchUsers(params = {}) {
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

  @action
  closePopup() {
    this.selectedUser = null;
  }
}
