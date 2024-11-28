import Route from '@ember/routing/route';

export default class OUsRoute extends Route {
  async model() {
    try {
      const response = await fetch('http://localhost:8080/backend_war_exploded/OUServlet');
      if (!response.ok) {
        throw new Error(`Failed to fetch OUs: ${response.statusText}`);
      }
      const ous = await response.json();
      return ous;
    } catch (error) {
      console.error('Error fetching OUs:', error);
      return [];
    }
  }

  setupController(controller, model) {
    super.setupController(controller, model);
    controller.ous = model;
  }
}
