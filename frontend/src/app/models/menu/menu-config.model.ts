export class MenuConfig {
  public className: string;
  public link: string;
  public title: any;
  
  constructor(className: string, link: string, title: any) {
    this.className = className;
    this.link = link;
    this.title = title;
  }
}