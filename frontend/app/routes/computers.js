import Route from '@ember/routing/route';

export default class ComputersRoute extends Route {
  async model() {
    try {
      const response = await fetch('http://localhost:8080/backend_war_exploded/ComputerServlet');
      if (!response.ok) {
        throw new Error(`Failed to fetch computers: ${response.statusText}`);
      }
      const computers = await response.json();
      return computers;
    } catch (error) {
      console.error('Error fetching computers:', error);
      return [];
    }
  }

  setupController(controller, model) {
    super.setupController(controller, model);
    controller.computers = model;
  }
}
