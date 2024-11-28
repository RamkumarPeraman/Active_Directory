import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class OUsController extends Controller {
  @tracked ous = [];
  @tracked selectedOU = null;

  @action
  async showOUDetails(ouId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/OUServlet/${ouId}`,
      );
      if (!response.ok) {
        throw new Error(`Failed to fetch OU details: ${response.statusText}`);
      }
      this.selectedOU = await response.json();
    } catch (error) {
      console.error('Error fetching OU details:', error);
      this.selectedOU = null;
    }
  }

  @action
  closePopup() {
    this.selectedOU = null;
  }
}
