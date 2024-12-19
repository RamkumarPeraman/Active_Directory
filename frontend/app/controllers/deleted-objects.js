import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class DeletedObjectsController extends Controller {
  @tracked deletedObjects = [];
  @tracked selectedDeletedObject = null;
  @action
  async showDeletedObjectsDetails(deletedObjectId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/DeletedObjServlet?id=${deletedObjectId}`
      );
  
      if (!response.ok) {
        throw new Error(`Failed to fetch deleted object details: ${response.statusText}`);
      }
  
      const deletedObjectDetails = await response.json();
        console.log('Fetched Deleted Object Details:', deletedObjectDetails);
  
      if (Array.isArray(deletedObjectDetails) && deletedObjectDetails.length > 0) {
        const firstDeletedObject = deletedObjectDetails[0];
        if (firstDeletedObject.name && firstDeletedObject.description) {
          this.selectedDeletedObject = firstDeletedObject;
        } else {
          this.selectedDeletedObject = null;
          console.error('The fetched data does not contain required name or description');
        }
      } else {
        this.selectedDeletedObject = null;
        console.error('No deleted object found in the response');
      }
    } catch (error) {
      console.error('Error fetching deleted object details:', error);
      this.selectedDeletedObject = null;
    }
  }
  @action
  closePopup() {
    this.selectedDeletedObject = null;
  }
  
  
}
