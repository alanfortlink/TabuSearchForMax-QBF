package problems.qbf.solvers;




import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import metaheuristics.tabusearch.AbstractTS;
import problems.Evaluator.EvaluateType;
import problems.qbf.QBF_Inverse;
import solutions.Solution;



/**
 * Metaheuristic TS (Tabu Search) for obtaining an optimal solution to a QBF
 * (Quadractive Binary Function -- {@link #QuadracticBinaryFunction}).
 * Since by default this TS considers minimization problems, an inverse QBF
 *  function is adopted.
 * 
 * @author ccavellucci, fusberti
 */
public class TS_QBF extends AbstractTS<Integer> {

	private final Integer fake = new Integer(-1);

	// Declarando os pares para fazer o first improment
	List<Pair<Integer, Integer>> provablePairs = new ArrayList<Pair<Integer, Integer>>();

	/**
	 * Constructor for the TS_QBF class. An inverse QBF objective function is
	 * passed as argument for the superclass constructor.
	 * 
	 * @param tenure
	 *            The Tabu tenure parameter.
	 * @param iterations
	 *            The number of iterations which the TS will be executed.
	 * @param filename
	 *            Name of the file for which the objective function parameters
	 *            should be read.
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public TS_QBF(Integer tenure, Integer iterations, String filename, EvaluateType evaluateType, LocalSearchMethod localSearchMethod) throws IOException {
		super(new QBF_Inverse(filename, evaluateType), tenure, iterations);

		this.localSearchMethod = localSearchMethod;
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeCL()
	 */
	@Override
	public ArrayList<Integer> makeCL() {

		ArrayList<Integer> _CL = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = new Integer(i);
			_CL.add(cand);
		}

		return _CL;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeRCL()
	 */
	@Override
	public ArrayList<Integer> makeRCL() {

		ArrayList<Integer> _RCL = new ArrayList<Integer>();

		return _RCL;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeTL()
	 */
	@Override
	public ArrayDeque<Integer> makeTL() {

		ArrayDeque<Integer> _TS = new ArrayDeque<Integer>(2*tenure);
		for (int i=0; i<2*tenure; i++) {
			_TS.add(fake);
		}

		return _TS;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#updateCL()
	 */
	@Override
	public void updateCL() {

		ArrayList<Integer> defaultCL = makeCL();
		ArrayList<Integer> newCL = new ArrayList<>();

		for(Integer item : defaultCL){
			if( incumbentSol.contains(item + 1) || incumbentSol.contains(item - 1) || incumbentSol.contains(item)){
				continue;
			}

			newCL.add(item);
		}

		this.CL = newCL;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		return sol;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The local search operator developed for the QBF objective function is
	 * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
	 */
	@Override
	public Solution<Integer> neighborhoodMove() {

		Double minDeltaCost;
		Integer NULL = 10000;
		Integer bestCandIn = null, bestCandOut = null;
		provablePairs.clear();

		minDeltaCost = Double.POSITIVE_INFINITY;
		updateCL();

		//	boolean foundBetterSolution = false;
		// Evaluate insertions
		for (Integer candIn : CL) {

			provablePairs.add(Pair.createPair(candIn, NULL));
			
		}
		// Evaluate removals
		for (Integer candOut : incumbentSol) {

			provablePairs.add(Pair.createPair(NULL, candOut));

		}
		// Evaluate exchanges
		for (Integer candIn : CL) {
			for (Integer candOut : incumbentSol) {

				provablePairs.add(Pair.createPair(candIn, candOut));




			}
		}

		Collections.shuffle(provablePairs);
		//nas minhas possiveis solucoes
		//eu pego uma e vejo se ela eh viavel

		for(Pair<Integer, Integer> pair : provablePairs) {
			//verifico se for insercao, remocao ou troca
			Double deltaCost = null;
			

			if(pair.getLeft() == NULL || pair.getRight() == NULL){
				//insert
				if(pair.getRight() == NULL){
					
					
					deltaCost = ObjFunction.evaluateInsertionCost(pair.getLeft(), incumbentSol);
					if (!TL.contains(pair.getLeft()) || incumbentSol.cost+deltaCost < bestSol.cost) {
						if (deltaCost < minDeltaCost) {
							minDeltaCost = deltaCost;
							bestCandIn = pair.getLeft();
							bestCandOut = null;
						}
					}


				}
				//remocao
				else{

					
					deltaCost = ObjFunction.evaluateRemovalCost(pair.getRight(), incumbentSol);
					if (!TL.contains(pair.getRight()) || incumbentSol.cost+deltaCost < bestSol.cost) {
						if (deltaCost < minDeltaCost) {
							minDeltaCost = deltaCost;
							bestCandIn = null;
							bestCandOut = pair.getRight();
						}
					}

				}


			}	//troca
			else
			{
				
				deltaCost = ObjFunction.evaluateExchangeCost(pair.getLeft(), pair.getRight(), incumbentSol);
				if ((!TL.contains(pair.getLeft()) && !TL.contains(pair.getRight())) || incumbentSol.cost+deltaCost < bestSol.cost) {
					if (deltaCost < minDeltaCost) {
						minDeltaCost = deltaCost;
						bestCandIn = pair.getLeft();
						bestCandOut = pair.getRight();
						
					}
				}
			}
			
			//Aqui eu estou usando os dois metodos
			//se chegar como send first e se entrar eu paro a busca
			//se chegar como best, entao este codicao nunca sera satisfeita
			if (minDeltaCost < 0.0 && localSearchMethod == LocalSearchMethod.FIRST_IMPROVING )  break;


		}

		// Implement the best non-tabu move
		TL.poll();
		if (bestCandOut != null) {
			incumbentSol.remove(bestCandOut);
			CL.add(bestCandOut);
			TL.add(bestCandOut);
		} else {
			TL.add(fake);
		}

		TL.poll();
		if (bestCandIn != null) {
			incumbentSol.add(bestCandIn);
			CL.remove(bestCandIn);
			TL.add(bestCandIn);
		} else {
			TL.add(fake);
		}
		ObjFunction.evaluate(incumbentSol);

		return null;
	}

	/**
	 * A main method used for testing the TS metaheuristic.
	 * 
	 */
	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		TS_QBF tabusearch = new TS_QBF(20, 100000, "instances/qbf100", EvaluateType.DEFAULT, LocalSearchMethod.BEST_IMPROVING);
		Solution<Integer> bestSol = tabusearch.solve();

		System.out.println("maxVal = " + bestSol);

		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = "+(double)totalTime/(double)1000+" seg");

	}

}
