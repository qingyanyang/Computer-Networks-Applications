import java.util.*;
//this is edge class, include neighbor and cost
class NeighborSH{
    private String neighbor;
    private int cost;
    public NeighborSH(String neighbor, int cost) {
        this.neighbor = neighbor;
        this.cost = cost;
    }
    public String getNeighborSH() {
        return neighbor;
    }
    public void setNeighborSH(String neighbor) {
        this.neighbor = neighbor;
    }
    public int getCost() {
        return cost;
    }
    public void setCost(int cost) {
        this.cost = cost;
    }
}
//this is graph class (map<router0,list({neighbour,cost})>...), which holds the whole net info
class GraphSH{
    private Map<String, List<NeighborSH>> net;
    public GraphSH(){
        this.net = new HashMap<>();
    }
    public Map<String, List<NeighborSH>> getNet() {
        return net;
    }

    public void setNet(Map<String, List<NeighborSH>> net) {
        this.net = net;
    }
    public void addNode(String node){
        this.net.put(node,new ArrayList<>());
    }
    public void addEdge(String sourceNode, String neighborNode, int weight){
        if(weight!=-1){
            this.net.computeIfAbsent(sourceNode, k -> new ArrayList<>());
            this.net.get(sourceNode).add(new NeighborSH(neighborNode,weight));
            this.net.computeIfAbsent(neighborNode, k -> new ArrayList<>());
            this.net.get(neighborNode).add(new NeighborSH(sourceNode,weight));
        }else{
            UpdateEdge(sourceNode,neighborNode,weight);
        }
    }
    public void UpdateEdge(String sourceNode, String neighborNode, int weight){
        //remove
        if(weight== -1){
            //get its neighbors and need to find corresponding one
            List<NeighborSH> neighborsSource = this.net.get(sourceNode);
            for(int i = 0; i< neighborsSource.size();i++){
                NeighborSH neighborObj = neighborsSource.get(i);
                if(neighborNode.equals(neighborObj.getNeighborSH())){
                    neighborsSource.remove(neighborObj);
                }
            }
            List<NeighborSH> neighbors = this.net.get(neighborNode);
            for(int i = 0; i< neighbors.size();i++){
                NeighborSH neighborObj = neighbors.get(i);
                if(sourceNode.equals(neighborObj.getNeighborSH())){
                    neighbors.remove(neighborObj);
                }
            }
        }else{
            //add new one
            //update
            this.addEdge(sourceNode,neighborNode,weight);
        }
    }
}
//this is class for holding distance table slot data, via router(key) has cost(value)
class DistanceListSH{
    private Map<String,String> distanceList;
    public DistanceListSH(){
        this.distanceList=new HashMap<>();
    }

    public Map<String, String> getDistanceListSH() {
        return distanceList;
    }

    public void setDistanceListSH(Map<String, String> distanceList) {
        this.distanceList = distanceList;
    }
}
//class for SH implementation
public class SplitHorizon {
    //global var
    static int t = 0;
    //for align printing
    public static void printFive(String occupy){
        int len = occupy.length();
        System.out.print(occupy);
        for(int i = 0; i< 5-len;i++){
            System.out.print(" ");
        }
    }
    //this method is for clone minCost table, because in one iteration, all router use the same minCost table,
    //but at the same time, there should be a table to record change.
    public static void cloneMinCost(NeighborSH[][] minCostRenew,NeighborSH[][] minCost){
        int len = minCost.length;
        for(int i = 0; i < len; i++){
            for(int j = 0; j < len; j++){
                minCostRenew[i][j]=minCost[i][j];
            }
        }
    }
    //compare row of distance table to get min cost one (via router and cost)
    public static NeighborSH getMin(DistanceListSH [][] distanceTable,String node,String desKey,Map<String,Integer> routeToIndex){
        DistanceListSH mapTemp = distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)];
        //compare to get min value
        int min = Integer.MAX_VALUE;
        NeighborSH minCompare = new NeighborSH("",min);
        int countINF = 0;
        for (String key : mapTemp.getDistanceListSH().keySet()) {
            String value = mapTemp.getDistanceListSH().get(key);
            if(value.equals("INF")){
                countINF++;
            }else{
                int costInt = Integer.parseInt(value);
                if(min>costInt){
                    minCompare.setCost(costInt);
                    minCompare.setNeighborSH(key);
                    min = costInt;
                }else if(min==costInt){
                    if(minCompare.getNeighborSH().compareTo(key)>0){
                        minCompare.setNeighborSH(key);
                    }
                }
            }
        }
        // if all IFN, return -1, minCost[][]=null
        if(countINF==mapTemp.getDistanceListSH().size()){
            return null;
        }else{
            return minCompare;
        }
    }
    //method to print distance tables
    public static void printDistanceTable(Set<String> keys,DistanceListSH [][] distanceTable,Map<String,Integer> routeToIndex){
        for (String node: keys) {
            System.out.println(node + " Distance Table at t=" + t);
            //first row title
            printFive(" ");
            for(String key:keys){
                if(!key.equals(node)){
                    printFive(key);
                }
            }
            System.out.println("");
            //des and values
            for(String desKey:keys) {
                if (!desKey.equals(node)) {
                    printFive(desKey);
                    for (String viaKey : keys) {
                        if (!viaKey.equals(node)) {
                            printFive(distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)].getDistanceListSH().get(viaKey));
                        }
                    }
                    System.out.println("");
                }
            }
            System.out.println("");
        }
    }
    //method to print routing tables
    public static void printRoutingTable(Set<String> keys,NeighborSH[][] minCost,Map<String,Integer> routeToIndex){
        for (Map.Entry<String, Integer> entry : routeToIndex.entrySet()) {
            String node = entry.getKey();
            int index = entry.getValue();
            System.out.println(node + " Routing Table:");
            for(String key:keys){
                if(!key.equals(node)){
                    NeighborSH viaObj = minCost[index][routeToIndex.get(key)];
                    String viaRouter = viaObj==null?"INF":viaObj.getNeighborSH();
                    String minCostStr =  viaObj==null?"INF": ""+viaObj.getCost();
                    System.out.print(key + "," + viaRouter +","+minCostStr+"\n");
                }
            }
            System.out.println("");
        }
    }
    // DV: neighbour cost table(c(x,y)) + minCost table(D(y,z)) = distance table-> update minCost table
    // + SH: D(x,z) = c(x,y) + D(y,z), if minCost table(D(y,z)) via x, then D(x,z) should be pre value of distance table (D(x,z) via y)
    // + but if there is disconnection of y and z, then D(y,z) = INF -> D(x,z) = INF
    public static void splitHorizon(int[][] neighborsCost, NeighborSH[][] minCost, Map<String,Integer> routeToIndex,DistanceListSH [][] distanceTable){
        //get all nodes
        Set<String> keys = routeToIndex.keySet();
        int len = routeToIndex.size();
        //distance table converge
        //1. compare distance table first, 2. then decide to print it or not
        boolean flag = true;
        while(flag){
            //create a new minCost, cuz during one iteration, they all using the one fix minCost table,
            //there has to be a temp minCost to record update
            NeighborSH[][] minCostRenew = new NeighborSH[len][len];
            cloneMinCost(minCostRenew,minCost);
            //count distance table changes during one iteration
            int count=0;
            for (String node: keys) {
                //traverse keys exclude target node and put value on table
                for(String desKey:keys){
                    if(!desKey.equals(node)){
                        for(String viaKey:keys){
                            if(!viaKey.equals(node)){
                                int neighborCost = neighborsCost[routeToIndex.get(node)][routeToIndex.get(viaKey)];
                                //use the fixed minCost table during one iteration
                                NeighborSH minObj = minCost[routeToIndex.get(viaKey)][routeToIndex.get(desKey)];
                                int minCostD = (minObj==null)?-1:(minObj.getNeighborSH().equals(node)?-2:minObj.getCost());
                                String cost = "";
                                if(neighborCost==-1||minCostD==-1){
                                    cost="INF";
                                }else if(minCostD==-2){
                                    //advertises the disconnection
                                    if(neighborsCost[routeToIndex.get(viaKey)][routeToIndex.get(desKey)]==-1){
                                        cost="INF";
                                    }else {
                                        //tells nothing, use pre value in distance table
                                        cost = distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)].getDistanceListSH().get(viaKey);
                                    }
                                } else{
                                    int costInt = neighborCost+minCostD;
                                    cost = ""+costInt;
                                }
                                DistanceListSH distanceListTemp = distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)];
                                if(distanceListTemp==null || distanceListTemp.getDistanceListSH().get(viaKey)==null || !distanceListTemp.getDistanceListSH().get(viaKey).equals(cost)){
                                    if(distanceListTemp==null){
                                        distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)]=new DistanceListSH();
                                    }
                                    flag=true;
                                    distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)].getDistanceListSH().put(viaKey,cost);
                                } else{
                                    count++;
                                }
                            }
                        }
                        //temp minCost table to record the update value
                        minCostRenew[routeToIndex.get(node)][routeToIndex.get(desKey)] = getMin(distanceTable,node,desKey,routeToIndex);
                    }
                }
            }
            // if count no changes in distance table, set flag false and stop loop
            if(count==routeToIndex.size()*(routeToIndex.size()-1)*(routeToIndex.size()-1)){
                flag=false;
                //last iteration before convergence t++, but it will stop then
                t--;
            }else{
                //print distance table now
                printDistanceTable(keys,distanceTable,routeToIndex);
            }
            //update t
            t++;
            //update minCost table for next iteration
            cloneMinCost(minCost,minCostRenew);
        }
        //Routing Table:
        printRoutingTable(keys,minCost,routeToIndex);
    }
    //method to make two-dimension arrays
    public static void getTables(int[][] neighborsCost,NeighborSH[][] minCost,Map<String,Integer> routeToIndex,Map<String, List<NeighborSH>>map){
        Set<String> keys = routeToIndex.keySet();
        //initialize
        for(String key:keys){
            minCost[routeToIndex.get(key)][routeToIndex.get(key)]=new NeighborSH(key,0);
        }

        for (Map.Entry<String, List<NeighborSH>> entry : map.entrySet()) {
            String node = entry.getKey();
            List<NeighborSH> neighbors = entry.getValue();
            for(NeighborSH neighbor : neighbors){
                neighborsCost[routeToIndex.get(node)][routeToIndex.get(neighbor.getNeighborSH())]=neighbor.getCost();
            }
        }
    }
    //method to get router to index map
    public static Map<String, Integer> getNeighborSHToIndex(GraphSH net){
        Map<String, List<NeighborSH>> map =  net.getNet();
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);

        //get every index map to routes
        Map<String, Integer> routeToIndex = new HashMap<>();
        int index = 0;
        for(String key:sortedKeys){
            routeToIndex.put(key,index);
            index++;
        }
        return routeToIndex;
    }
    //method to update min cost table
    public static void mergeMinCost(NeighborSH[][] minCost,Map<String,Integer> routeToIndex,Map<String,Integer> routeToIndexUpdate,NeighborSH[][] minCostUpdate,DistanceListSH [][] distanceTable,DistanceListSH [][] distanceTableUpdate){
        //traverse the pre routeToIndex, put pre value into new one
        for (Map.Entry<String, Integer> entry : routeToIndex.entrySet()) {
            String node = entry.getKey();
            for (Map.Entry<String, Integer> entry02 : routeToIndex.entrySet()) {
                String node02 = entry02.getKey();
                minCostUpdate[routeToIndexUpdate.get(node)][routeToIndexUpdate.get(node02)]=minCost[routeToIndex.get(node)][routeToIndex.get(node02)];
                distanceTableUpdate[routeToIndexUpdate.get(node)][routeToIndexUpdate.get(node02)]=distanceTable[routeToIndex.get(node)][routeToIndex.get(node02)];
            }
        }
    }
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        //using adjacencyList to store graph
        GraphSH net = new GraphSH();
        String userInput = input.next();
        while(!userInput.equals("DISTANCEVECTOR")){
            net.addNode(userInput);
            userInput = input.next();
        }
        userInput = input.next();
        while(!userInput.equals("UPDATE")){
            String firstNode = userInput;
            String secondNode = input.next();
            String weight = input.next();
            int cost =Integer.parseInt(weight);
            //put neighbor into net
            net.addEdge(firstNode,secondNode,cost);
            userInput=input.next();
        }
        Map<String, Integer> routeToIndex = getNeighborSHToIndex(net);
        //need two tables
        //create minCost table here, for global use
        int len = routeToIndex.size();
        //distance table
        DistanceListSH [][] distanceTable = new DistanceListSH [len][len];
        //default value is null
        NeighborSH[][] minCost = new NeighborSH[len][len];
        //one for searching neighbor cost,default value is -1
        int [][] neighborsCost = new int[len][len];
        for(int i = 0; i< len;i++){
            Arrays.fill(neighborsCost[i],-1);
        }
        //initialize tables
        getTables(neighborsCost,minCost,routeToIndex,net.getNet());
        //SH
        splitHorizon(neighborsCost, minCost, routeToIndex,distanceTable);
        //see if there are any user inputs
        boolean flag=false;
        userInput = input.next();
        while(!userInput.equals("END")){
            //check validation
            //A user may input 0 or more lines in the "UPDATE" section.
            //normal situation
            String firstNode = userInput;
            String secondNode = input.next();
            String weight = input.next();
            int cost =Integer.parseInt(weight);
            net.UpdateEdge(firstNode,secondNode,cost);
            flag=true;
            userInput = input.next();
        }
        if(flag){
            //output the res here
            Map<String, Integer> routeToIndexUpdate = getNeighborSHToIndex(net);
            //need two tables
            //create minCost table here, for global use
            int lenUpdate = routeToIndexUpdate.size();
            //default value is null
            NeighborSH[][] minCostUpdate = new NeighborSH[lenUpdate][lenUpdate];
            DistanceListSH [][] distanceTableUpdate = new DistanceListSH [lenUpdate][lenUpdate];
            //one for searching neighbor cost,default value is -1
            int [][] neighborsCostUpdate = new int[lenUpdate][lenUpdate];
            for(int i = 0; i< lenUpdate;i++){
                Arrays.fill(neighborsCostUpdate[i],-1);
            }
            //update 3 tables
            getTables(neighborsCostUpdate,minCostUpdate,routeToIndexUpdate,net.getNet());
            mergeMinCost(minCost,routeToIndex,routeToIndexUpdate,minCostUpdate,distanceTable,distanceTableUpdate);
            //SH
            splitHorizon(neighborsCostUpdate, minCostUpdate,routeToIndexUpdate,distanceTableUpdate);
        }
    }
}
