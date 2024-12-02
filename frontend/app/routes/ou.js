import Route from '@ember/routing/route';

export default class OURoute extends Route {
  async model() {
    try {
      const response = await fetch(
        'http://localhost:8080/backend_war_exploded/OUServlet',
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch organizational units: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching organizational units:', error);
      return [];
    }
  }
  setupController(controller, model) {
    super.setupController(controller, model);
    controller.ous = model;
  }
}
