
export class TreeNode {
    public searchCode: string;
    public code: string;
    public description: string;
    public leaf: boolean = false;
    public children: TreeNode[];
    expanded: boolean = true;


    constructor(searchCode: string, code: string, description: string, children?: TreeNode[]) {
        this.code = code;
        this.searchCode = searchCode;
        this.description = description;
        this.children = children;
        this.leaf = this.children == undefined || this.children.length == 0;
    }

    toggle() {
        this.expanded = !this.expanded;
    }
    
    clone() {
        let children = undefined
        if (this.children && this.children.length > 0) {
            children = this.children.map(child =>  child.clone() );
            return new TreeNode(this.searchCode, this.code, this.description, children);
        } 
        
        return new TreeNode(this.searchCode, this.code, this.description);
    }
    
    static cloneTree(treeNodeList : TreeNode[]) {
        return treeNodeList.map(node => node.clone() );
    }

    static fromJson(json : any) : TreeNode {
        return new TreeNode(
            json.searchCode, 
            json.code, 
            json.description, 
            json.children
            .map(child => {
                return TreeNode.fromJson(child)
            } ))
    }
}