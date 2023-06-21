import java.util.*;
//this is edge class, include neighbor and cost
class Neighbor{
    private String neighbor;
    private int cost;
    public Neighbor(String neighbor, int cost) {
        this.neighbor = neighbor;
        this.cost = cost;
    }
    public String getNeighbor() {
        return neighbor;
    }
    public void setNeighbor(String neighbor) {
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
class Graph{
    private Map<String, List<Neighbor>> net;
    public Graph(){
        this.net = new HashMap<>();
    }
    public Map<String, List<Neighbor>> getNet() {
        return net;
    }

    public void setNet(Map<String, List<Neighbor>> net) {
        this.net = net;
    }
    public void addNode(String node){
        this.net.put(node,new ArrayList<>());
    }
    public void addEdge(String sourceNode, String neighborNode, int weight){
        if(weight!=-1){
            this.net.computeIfAbsent(sourceNode, k -> new ArrayList<>());
            this.net.get(sourceNode).add(new Neighbor(neighborNode,weight));
            this.net.computeIfAbsent(neighborNode, k -> new ArrayList<>());
            this.net.get(neighborNode).add(new Neighbor(sourceNode,weight));
        }else{
            UpdateEdge(sourceNode,neighborNode,weight);
        }
    }
    public void UpdateEdge(String sourceNode, String neighborNode, int weight){
        //remove
        if(weight== -1){
            //get its neighbors and need to find corresponding one
            List<Neighbor> neighborsSource = this.net.get(sourceNode);
            for(int i = 0; i< neighborsSource.size();i++){
                Neighbor neighborObj = neighborsSource.get(i);
                if(neighborNode.equals(neighborObj.getNeighbor())){
                    neighborsSource.remove(neighborObj);
                }
            }
            List<Neighbor> neighbors = this.net.get(neighborNode);
            for(int i = 0; i< neighbors.size();i++){
                Neighbor neighborObj = neighbors.get(i);
                if(sourceNode.equals(neighborObj.getNeighbor())){
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
class DistanceList{
    private Map<String,String> distanceList;
    public DistanceList(){
        this.distanceList=new HashMap<>();
    }

    public Map<String, String> getDistanceList() {
        return distanceList;
    }

    public void setDistanceList(Map<String, String> distanceList) {
        this.distanceList = distanceList;
    }
}
//class for DV implementation
public class DistanceVector {
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
    public static void cloneMinCost(Neighbor[][] minCostRenew,Neighbor[][] minCost){
        int len = minCost.length;
        for(int i = 0; i < len; i++){
            for(int j = 0; j < len; j++){
                minCostRenew[i][j]=minCost[i][j];
            }
        }
    }
    //compare row of distance table to get min cost one (via router and cost)
    public static Neighbor getMin(DistanceList [][] distanceTable,String node,String desKey,Map<String,Integer> routeToIndex){
        DistanceList mapTemp = distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)];
        //compare to get min value
        int min = Integer.MAX_VALUE;
        Neighbor minCompare = new Neighbor("",min);
        int countINF = 0;
        for (String key : mapTemp.getDistanceList().keySet()) {
            String value = mapTemp.getDistanceList().get(key);
            if(value.equals("INF")){
                countINF++;
            }else{
                int costInt = Integer.parseInt(value);
                if(min>costInt){
                    minCompare.setCost(costInt);
                    minCompare.setNeighbor(key);
                    min = costInt;
                }else if(min==costInt){
                    if(minCompare.getNeighbor().compareTo(key)>0){
                        minCompare.setNeighbor(key);
                    }
                }
            }
        }
        // if all IFN, return -1, minCost[][]=null
        if(countINF==mapTemp.getDistanceList().size()){
            return null;
        }else{
            return minCompare;
        }
    }
    //method to print distance tables
    public static void printDistanceTable(Set<String> keys,DistanceList [][] distanceTable,Map<String,Integer> routeToIndex){
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
                            printFive(distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)].getDistanceList().get(viaKey));
                        }
                    }
                    System.out.println("");
                }
            }
            System.out.println("");
        }
    }
    //method to print routing tables
    public static void printRoutingTable(Set<String> keys,Neighbor[][] minCost,Map<String,Integer> routeToIndex){
        for (Map.Entry<String, Integer> entry : routeToIndex.entrySet()) {
            String node = entry.getKey();
            int index = entry.getValue();
            System.out.println(node + " Routing Table:");
            for(String key:keys){
                if(!key.equals(node)){
                    Neighbor viaObj = minCost[index][routeToIndex.get(key)];
                    String viaRouter = viaObj==null?"INF":viaObj.getNeighbor();
                    String minCostStr =  viaObj==null?"INF": ""+viaObj.getCost();
                    System.out.print(key + "," + viaRouter +","+minCostStr+"\n");
                }
            }
            System.out.println("");
        }
    }
    //DV: neighbour cost table(c(x,y)) + minCost table(D(y,z))= distance table-> update minCost table
    public static void distanceVictor(int[][] neighborsCost, Neighbor[][] minCost, Map<String,Integer> routeToIndex,DistanceList [][] distanceTable){
        //get all nodes
        Set<String> keys = routeToIndex.keySet();
        int len = routeToIndex.size();
        //distance table converge
        //1. compare distance table first (distance table converge) 2. then decide to print it or not
        boolean flag = true;
        while(flag){
            //create a new minCost, cuz during one iteration, they all using the one fix minCost table,
            //there has to be a temp minCost to record update
            Neighbor[][] minCostRenew = new Neighbor[len][len];
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
                                Neighbor minObj = minCost[routeToIndex.get(viaKey)][routeToIndex.get(desKey)];
                                int minCostD = (minObj==null)?-1:minObj.getCost();
                                String cost = "";
                                if(neighborCost==-1||minCostD==-1){
                                    cost="INF";
                                }else{
                                    int costInt = neighborCost+minCostD;
                                    cost = ""+costInt;
                                }
                                DistanceList distanceListTemp = distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)];
                                //check if the distance table changed
                                if(distanceListTemp==null || distanceListTemp.getDistanceList().get(viaKey)==null || !distanceListTemp.getDistanceList().get(viaKey).equals(cost)){
                                    if(distanceListTemp==null){
                                        distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)]=new DistanceList();
                                    }
                                    flag=true;
                                    //record info
                                    distanceTable[routeToIndex.get(node)][routeToIndex.get(desKey)].getDistanceList().put(viaKey,cost);
                                } else{
                                    //record unchanged times
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
    public static void getTables(int[][] neighborsCost,Neighbor[][] minCost,Map<String,Integer> routeToIndex,Map<String, List<Neighbor>>map){
        Set<String> keys = routeToIndex.keySet();
        //initialize
        for(String key:keys){
            minCost[routeToIndex.get(key)][routeToIndex.get(key)]=new Neighbor(key,0);
        }

        for (Map.Entry<String, List<Neighbor>> entry : map.entrySet()) {
            String node = entry.getKey();
            List<Neighbor> neighbors = entry.getValue();
            for(Neighbor neighbor : neighbors){
                neighborsCost[routeToIndex.get(node)][routeToIndex.get(neighbor.getNeighbor())]=neighbor.getCost();
            }
        }
    }
    //method to get router to index map
    public static Map<String, Integer> getNeighborToIndex(Graph net){
        Map<String, List<Neighbor>> map =  net.getNet();
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
    public static void mergeMinCost(Neighbor[][] minCost,Map<String,Integer> routeToIndex,Map<String,Integer> routeToIndexUpdate,Neighbor[][] minCostUpdate,DistanceList [][] distanceTable,DistanceList [][] distanceTableUpdate){
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
        Graph net = new Graph();
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
        Map<String, Integer> routeToIndex = getNeighborToIndex(net);
        //need two tables
        //create minCost table here, for global use
        int len = routeToIndex.size();
        //distance table
        DistanceList [][] distanceTable = new DistanceList [len][len];
        //default value is null
        Neighbor[][] minCost = new Neighbor[len][len];
        //one for searching neighbor cost,default value is -1
        int [][] neighborsCost = new int[len][len];
        for(int i = 0; i< len;i++){
            Arrays.fill(neighborsCost[i],-1);
        }
        //initialize tables
        getTables(neighborsCost,minCost,routeToIndex,net.getNet());
        //DV
        distanceVictor(neighborsCost, minCost, routeToIndex,distanceTable);
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
            Map<String, Integer> routeToIndexUpdate = getNeighborToIndex(net);
            //need two tables
            //create minCost table here, for global use
            int lenUpdate = routeToIndexUpdate.size();
            //default value is null
            Neighbor[][] minCostUpdate = new Neighbor[lenUpdate][lenUpdate];
            DistanceList [][] distanceTableUpdate = new DistanceList [lenUpdate][lenUpdate];
            //one for searching neighbor cost,default value is -1
            int [][] neighborsCostUpdate = new int[lenUpdate][lenUpdate];
            for(int i = 0; i< lenUpdate;i++){
                Arrays.fill(neighborsCostUpdate[i],-1);
            }
            //update 3 tables
            getTables(neighborsCostUpdate,minCostUpdate,routeToIndexUpdate,net.getNet());
            mergeMinCost(minCost,routeToIndex,routeToIndexUpdate,minCostUpdate,distanceTable,distanceTableUpdate);
            //DV
            distanceVictor(neighborsCostUpdate, minCostUpdate,routeToIndexUpdate,distanceTableUpdate);
        }
    }
}
