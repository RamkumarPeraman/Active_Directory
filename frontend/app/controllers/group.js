import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class GroupController extends Controller {
  @tracked groups = [];
  @tracked selectedGroup = null;
  @tracked sortBy = '';
  @tracked searchQuery = ''; 

  @action
  async fetchGroups(params = {}) {
    params.sortBy = this.sortBy; 
    params.search = this.searchQuery;
    const query = new URLSearchParams(params).toString();
    const url = `http://localhost:8080/backend_war_exploded/GroupServlet?${query}`;
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to fetch groups: ${response.statusText}`);
      }
      this.groups = await response.json();
    } catch (error) {
      console.error('Error fetching groups:', error);
      this.groups = [];
    }
  }
  @action
  async showGroupDetails(groupId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/GroupServlet?id=${groupId}`,
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

  @action
  updateSortBy(event) {
    this.sortBy = event.target.value;
    this.fetchGroups();
  }

  @action
  updateSearchQuery(event) {
    this.searchQuery = event.target.value;
    this.fetchGroups(); 
  }
}


