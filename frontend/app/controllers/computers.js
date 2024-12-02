import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class ComputerController extends Controller {
  @tracked computers = [];
  @tracked selectedComputer = null;

  @action
  async fetchComputers(params = {}) {
    const query = new URLSearchParams(params).toString();
    const url = `http://localhost:8080/backend_war_exploded/ComputerServlet?${query}`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to fetch computers: ${response.statusText}`);
      }
      this.computers = await response.json();
    } catch (error) {
      console.error('Error fetching computers:', error);
      this.computers = [];
    }
  }

  @action
  async showComputerDetails(computerId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/ComputerServlet?id=${computerId}`,
      );
      if (!response.ok) {
        throw new Error(`Failed to fetch computer details: ${response.statusText}`);
      }
      this.selectedComputer = await response.json();
    } catch (error) {
      console.error('Error fetching computer details:', error);
      this.selectedComputer = null;
    }
  }

  @action
  closePopup() {
    this.selectedComputer = null;
  }
}
