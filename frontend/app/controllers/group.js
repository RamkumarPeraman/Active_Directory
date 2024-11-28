import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class GroupsController extends Controller {
  @tracked groups = [];
  @tracked selectedGroup = null;

  @action
  async showGroupDetails(groupId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/GroupServlet/${groupId}`,
      );
      if (!response.ok) {
        throw new Error(
          `Failed to fetch group details: ${response.statusText}`,
        );
      }
      this.selectedGroup = await response.json();
    } catch (error) {
      console.error('Error fetching group details:', error);
      this.selectedGroup = null;
    }
  }

  @action
  closePopup() {
    this.selectedGroup = null;
  }
}
