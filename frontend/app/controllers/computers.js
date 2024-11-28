import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class ComputersController extends Controller {
  @tracked computers = [];
  @tracked selectedComputer = null;

  @action
  async showComputerDetails(computerId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/ComputerServlet/${computerId}`,
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
