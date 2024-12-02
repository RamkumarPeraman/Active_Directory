import Route from '@ember/routing/route';

export default class GroupRoute extends Route {
  async model() {
    try {
      const response = await fetch(
        'http://localhost:8080/backend_war_exploded/GroupServlet',
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch groups: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching groups:', error);
      return [];
    }
  }
  setupController(controller, model) {
    super.setupController(controller, model);
    controller.groups = model;
  }
}
